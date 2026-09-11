# 支付成功库存异常整单自动退款设计

> 历史设计：本文件的整单预检、全额补偿和 ANOMALY_PENDING 防误释放原则已合并进 [统一业务设计](2026-09-04-order-payment-refund-inventory-design.md)。新实现使用独立 Reservation、按 paymentNo 绑定补偿退款、独立付款/订单状态；本文件的旧字段、按 orderNo 补偿键及自动退款增加商品 refunded_quantity 的规则不再生效。

## 1. 目标与边界

当支付渠道已经确认整张订单支付成功，但本地准备执行 `locked_stock -> sold_stock` 时发现任一订单明细没有足够的锁定库存，系统必须停止整单库存实扣，并自动发起覆盖全部订单明细和全部支付金额的退款。

本设计只处理支付完成阶段发现的商品聚合锁定库存不足。商品或订单明细缺失、`NOTPAY` 订单明细不是 `RESERVED` 等情况属于数据完整性故障，不能进入自动退款事务。正常用户退款仍按用户选择的订单明细和数量处理；渠道退款失败不会伪造退款成功；库存异常不会通过增加库存掩盖。

## 2. 方案选择

采用“支付本地完成事务内持久化补偿退款，再异步调用退款渠道”的方案：

- 不在支付回调线程同步调用退款渠道，避免回调超时和远程事务。
- 不先提交部分商品库存，任一明细失败时整单不产生 `ORDER_COMMIT` 库存变化。
- 不把渠道已支付订单恢复成未支付或已关闭；支付流水保留渠道事实，订单进入 `REFUND_PROCESSING`。
- 自动退款与现有 `REFUND_SUBMIT_REQUESTED` Outbox、Inbox 和固定 `refundNo` 渠道幂等链路复用。

未采用的方案：

- 支付回调内同步退款：渠道延迟或超时会拖垮支付通知，并产生无法原子提交的远程调用窗口。
- 先把订单标为支付成功，再后台重试扣库存：会暂时暴露不可履约订单，并可能出现部分商品已扣、部分未扣。
- 只退款缺货明细：会把一次整单支付改成隐式部分履约，与已确认的整单退款口径不符。

## 3. 数据模型

### 3.1 退款来源

`t_refund_info` 新增：

```text
refund_source varchar(32) NOT NULL DEFAULT 'USER'
```

取值：

- `USER`：用户按订单明细申请。
- `PAYMENT_STOCK_SHORTAGE`：支付成功后库存提交预检失败，系统生成的整单退款。

自动退款使用稳定请求号 `AUTO_STOCK_REFUND:{deterministicUuid(orderNo)}`，其中 `deterministicUuid` 是由订单号生成的 36 位名称型 UUID，因此完整请求号长度为 54，满足 `application_request_id varchar(64)` 约束；该请求号同时受现有 `(order_no, application_request_id)` 唯一约束保护。自动退款创建时直接设置 `approval_status=APPROVED`、`refund_status=CREATED`，退款金额严格等于订单支付金额，退款明细覆盖订单全部明细及其完整购买数量。

### 3.2 订单明细库存状态

`t_order_item.inventory_status` 增加两个状态：

- `ANOMALY_PENDING`：支付完成预检时，该商品聚合锁定库存小于本订单对该商品的聚合需求量，正在等待自动退款。同一商品在本订单内的全部 `RESERVED` 明细必须一起转入该状态；即使其他订单后来增加了同一商品的聚合锁定库存，也不得用它替本订单释放库存。
- `ANOMALY`：渠道退款已经成功，但该明细没有可安全释放的完整预占量。系统不得通过增加可用库存修复该状态。

`ANOMALY_PENDING` 长度为 15，满足现有 `inventory_status varchar(16)` 约束。

自动退款成功后的明细结果：

- 首次支付预检和退款完成时均可证明聚合锁定库存足够：`RESERVED -> RELEASED`。
- 首次支付预检已经不足：同一商品的全部明细 `RESERVED -> ANOMALY_PENDING -> ANOMALY`，无论随后聚合锁定库存是否增加都不释放。
- 首次预检通过但退款完成时锁定库存意外不足：`RESERVED -> ANOMALY`。
- 关单或取消已先完成释放：保持 `RELEASED`，不再改变库存。

两种结果都把该自动退款明细计入 `refunded_quantity`，因为该字段表示已确认的退款数量，而不是库存修复结果。

### 3.3 库存流水

新增操作类型和唯一业务键：

```text
PAYMENT_REFUND_RELEASE:{refundNo}:{orderItemId}
PAYMENT_STOCK_ANOMALY:{orderNo}:{productId}
```

`PAYMENT_REFUND_RELEASE` 记录 `locked_stock -> available_stock`。`PAYMENT_STOCK_ANOMALY` 的三个库存变化量均为 0，保存期望数量、实际锁定数量和自动退款单号，供管理员核查；它不能改变库存。

## 4. 支付完成事务

微信 V2、微信 V3、支付宝通知与主动查单统一调用本地支付完成服务。验签、商户、订单号和金额校验在事务外完成；本地事务执行：

1. 用稳定渠道事件 ID 认领 Inbox，并锁定订单。
2. 先按订单状态裁决重复与竞态：`SUCCESS/PARTIAL_REFUND/REFUND_PROCESSING/REFUND_ABNORMAL/REFUND_SUCCESS` 均属于已经记录支付事实的状态，直接返回既有支付或退款结果；只有 `NOTPAY` 进入库存提交预检。若渠道已确认支付成功但本地订单已经是 `CLOSED/CANCEL`，说明预占库存可能已经释放，必须直接进入自动退款，不得恢复订单或重新占用库存。
3. 对 `NOTPAY` 订单锁定全部订单明细，并按商品 ID 升序锁定商品行。全部明细必须为 `RESERVED` 且商品必须存在；违反任一条件属于数据完整性故障，事务回滚并向渠道返回失败，不创建自动退款。
4. 在改变订单状态或库存前，按商品聚合本订单所需数量，预检每个商品是否满足 `locked_stock >= requiredQuantity`。
5. 预检全部通过时，执行既有正常路径：订单 `NOTPAY -> SUCCESS`、全部明细 `RESERVED -> SOLD`、`locked_stock -> sold_stock`、支付流水、`ORDER_COMMIT` 流水和 Inbox `CONSUMED` 同事务提交。
6. 任一商品的聚合锁定库存不足时，不执行任何 `ORDER_COMMIT` 更新；把该商品在本订单内的全部 `RESERVED` 明细一起标记为 `ANOMALY_PENDING`，其他商品明细保持 `RESERVED`。随后保存支付流水，将订单 `NOTPAY -> REFUND_PROCESSING`，创建或复用整单自动退款及全部退款明细，写唯一 `REFUND_SUBMIT_REQUESTED:{refundNo}` Outbox，并把 Inbox 标为 `CONSUMED`。对已是 `CLOSED/CANCEL` 的迟到支付成功事件，必须先确认全部订单明细均为 `RELEASED`，再执行同一补偿事务并将订单更新为 `REFUND_PROCESSING`；状态不一致时按数据完整性故障回滚，不创建自动退款。
7. 补偿事务提交成功后，支付通知向渠道返回成功。自动退款或 Outbox 任一步失败时整个事务回滚，通知返回失败，由支付渠道重试。

重复支付事件看到订单已经进入任一已支付或退款状态时不得再次扣库存或创建退款；稳定 Inbox ID、自动退款请求号和 Outbox 事件键共同防重。

## 5. 自动退款提交与完成

退款提交消费者继续在数据库事务外用固定 `refundNo` 调用微信或支付宝退款接口。远程调用成功后，短事务写退款状态查询 Outbox 并完成 Inbox；进程在远程成功后崩溃时允许使用同一 `refundNo` 重放。

渠道确认自动退款成功后，退款完成服务先取得与支付、关单相同的统一订单锁，再在一个本地事务中：

1. 依次锁定订单行、退款单、退款明细和订单明细，再按商品 ID 升序锁定商品行。
2. 将渠道财务事实更新为 `refund_status=SUCCESS`。
3. 按商品聚合自动退款明细。只有一直保持 `RESERVED` 且退款完成时锁定库存仍足够的商品，才释放完整预占量并写 `PAYMENT_REFUND_RELEASE`。`ANOMALY_PENDING` 明细无论当前聚合锁定库存多少都不释放；保持 `RESERVED` 但此时锁定库存意外不足的商品也不释放，两者均写 `PAYMENT_STOCK_ANOMALY`。已是 `RELEASED` 的明细不再改变库存。
4. 将安全释放的订单明细更新为 `RELEASED`，`ANOMALY_PENDING` 或二次检查不足的更新为 `ANOMALY`，已是 `RELEASED` 的保持不变；全部自动退款明细增加对应 `refunded_quantity`。
5. 将订单更新为 `REFUND_SUCCESS` 并完成该渠道事件 Inbox。

库存异常不能阻止已经发生的渠道退款被记录为成功，也不能制造库存。管理员通过现有库存流水查询看到 `PAYMENT_STOCK_ANOMALY` 并进行人工盘点；后续人工库存调整仍必须使用 `ADMIN_ADJUST` 和独立请求号。

## 6. 错误与并发处理

- 预检持有与后续更新相同的商品行锁，预检成功后不存在锁定库存被并发抢走的窗口。
- 预检失败路径不捕获并吞掉任意数据库错误；只有订单明细与商品完整、明细全部为 `RESERVED`，但明确出现商品聚合锁定库存不足时才进入自动退款。商品或明细缺失、状态不一致及其他数据库异常均回滚、让支付事件重试并记录告警。
- 支付与关单竞争继续由订单行锁和状态条件更新裁决。支付事务先取得订单锁时，正常提交或自动退款完成后关单不得释放；关单已先提交 `CLOSED/CANCEL` 和库存释放时，随后确认的渠道支付成功必须转入自动退款。
- 自动退款提交、状态查询和成功通知允许重复到达；退款请求号、Outbox 事件键、Inbox、退款状态条件和库存流水业务键共同保证幂等。
- 自动退款处于 `FAILED`、`ABNORMAL` 或渠道状态未知时，订单保持 `REFUND_PROCESSING`，预占库存不释放，直到渠道确认成功或管理员处理。
- 后端创建用户退款申请时必须查询该订单是否已存在来源为 `PAYMENT_STOCK_SHORTAGE` 的自动退款；存在时直接拒绝用户退款。普通用户部分退款导致的 `REFUND_PROCESSING` 仍遵循原有可退数量规则，不受此限制。

## 7. 测试与验收

- 多明细订单中任一商品锁定库存不足时，所有商品均不产生 `ORDER_COMMIT`，自动退款金额等于订单总金额，退款明细覆盖全部购买数量。
- 补偿退款或 Outbox 写入失败时，订单、支付流水、退款记录和 Inbox 全部回滚，支付通知返回失败。
- 相同支付通知、主动查单或 Nginx 重试只生成一张自动退款单和一条退款提交 Outbox。
- 正常库存订单仍一次性完成全部 `RESERVED -> SOLD`。
- 自动退款未确认成功前不释放锁定库存。
- 首次预检不足的明细先标记 `ANOMALY_PENDING`；即使其他订单后来增加了相同商品的锁定库存，自动退款成功时也只转为 `ANOMALY`，不得释放其他订单的预占。
- 同一商品在一张订单中出现多条明细时，该商品聚合不足会将这些明细全部标记为 `ANOMALY_PENDING`；不得只标记部分明细。
- 首次和退款完成时均能证明完整存在的预占才会被释放；二次检查不足的明细标记 `ANOMALY` 且可用库存不增加。
- 自动退款成功重复通知不重复释放库存、不重复增加退款数量。
- 关单或取消已先释放库存、随后收到支付成功时，只创建一次整单自动退款，不重新占用或扣减库存；退款成功后相关明细保持 `RELEASED`，不得标记为库存异常。
- 商品/订单明细缺失或 `NOTPAY` 明细状态不是 `RESERVED` 时，不创建自动退款，支付完成事务回滚、回调返回失败并产生告警。
- 自动退款存在期间，后端拒绝新的用户退款申请；普通用户部分退款产生的 `REFUND_PROCESSING` 不被误判为自动退款。
- 微信 V2、微信 V3、支付宝通知和主动查单均覆盖正常、库存异常、重复事件和补偿落库失败测试。

## 8. 迁移与兼容

- `refund_source` 对历史退款回填 `USER`。
- `ANOMALY_PENDING/ANOMALY` 是订单明细库存状态的新增值，不改变现有 API 字段名称。
- 自动退款通过现有管理员退款列表展示，并明确标记“支付库存异常自动退款”；不提供管理员再次审批按钮。
- React 与 Vue 的普通订单页面在订单进入 `REFUND_PROCESSING` 时显示“支付成功，库存异常，正在全额退款”，不得继续发起用户退款。

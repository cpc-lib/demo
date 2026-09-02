# SPEC-010：商品库存管理与按明细退款兼容变更

## 元信息

| 字段 | 值 |
|---|---|
| Spec ID | SPEC-010 |
| 状态 | planned |
| 分类 | `type: compatibility` |
| 计划版本 | v0.5.0 |
| 设计日期 | 2026-08-31 |
| 依赖 | SPEC-008、SPEC-009；吸收 SPEC-004 的退款并发安全范围 |
| 影响范围 | 数据库、商品、购物车、订单、三支付渠道、退款、RabbitMQ、React、Vue、测试与文档 |

## 1. 背景与目标

当前 `t_product` 只有名称和价格，购物车与订单链路不校验库存，支付成功、关单和退款也不会改变库存。现有退款按订单金额申请，无法确定部分退款对应的商品数量，因此不能可靠回补库存。

本变更需要实现：

- ADMIN 管理商品、新增商品、编辑名称和价格、上架/下架；
- ADMIN 调整库存并查看完整库存操作记录；
- USER 只能查看并购买上架且有可用库存的商品；
- 所有用户订单创建入口抵御浏览器重复点击、Axios 重试、Nginx 重试和多实例并发；
- 结算创建订单时预占库存，支付成功后转为实际售出；
- 取消或确认超时关闭后释放预占库存；
- 按订单明细与数量申请退款，渠道确认成功后回补库存；
- 使用数据库事务、唯一约束、Outbox/Inbox 消息表和 RabbitMQ 控制并发与幂等；
- React、Vue 页面、字段、权限和错误行为保持一致。

## 2. 范围与非目标

### 2.1 本期范围

- 商品新增、编辑、上下架、库存调整、库存流水查询；
- 商品目录、购物车、结算和旧直购入口的库存校验；
- 购物车结算和旧直购入口统一订单创建幂等契约；
- 订单预占、支付实扣、关闭释放、退款成功回补；
- 微信 V2、微信 V3、支付宝支付成功入口统一库存处理；
- 新按明细退款请求，现有退款审核、渠道退款、通知、主动查单和状态核对复用；
- Outbox 可靠投递、Inbox 消费幂等、失败消息后台重投接口；
- React 与 Vue 的商品管理、商品目录、购物车、订单退款页面同步改造。

### 2.2 非目标

- 不提供商品物理删除；
- 购物车不预占库存；
- 不引入独立库存微服务或分布式数据库事务；
- 不兼容旧的按金额退款请求和渠道专用退款申请 URL；
- 不新增促销、优惠券、组合商品、仓库或多规格 SKU；
- 不引入新的数据库迁移框架。

## 3. 核心业务规则

### 3.1 商品状态与可见性

- 商品状态只有 `ON_SHELF`、`OFF_SHELF`。
- 普通商品列表只返回 `ON_SHELF` 商品；游客和 USER 均无法从目录查看下架商品。
- ADMIN 商品列表返回全部商品。
- `ON_SHELF` 且 `available_stock > 0` 才允许加入购物车。
- `ON_SHELF` 但库存为 0 的商品仍可展示，标记“已售罄”，不可加入购物车。
- 购物车中已经存在的下架、售罄或数量超过最新可用库存的商品保留为不可结算行，只能移除或调低到合法数量。
- 已经创建并预占库存的未支付订单不受后续下架影响，在原支付时限内仍可支付。

### 3.2 商品管理

- 新商品默认 `OFF_SHELF`，允许在创建时设置非负初始库存。
- 编辑名称或价格必须携带 `version`，版本冲突返回 HTTP 409。
- 上下架不改变任何库存数量。
- 库存调整只改变 `available_stock`，不得直接修改 `locked_stock` 或 `sold_stock`。
- 库存调整使用增量；负数调整后 `available_stock` 不得小于 0。
- 每次库存调整必须携带 `requestId` 和非空原因。
- 不提供删除接口；被订单或购物车引用的商品通过下架退出销售。

### 3.3 库存生命周期

- 购物车只是购买意向，不占库存；从购物车移除商品不改变库存。
- 结算成功创建未支付订单时执行 `available_stock -> locked_stock`。
- 渠道确认支付成功时执行 `locked_stock -> sold_stock`。
- 用户取消，或渠道确认超时订单未支付/已关闭时执行 `locked_stock -> available_stock`。
- 渠道状态不明确时保留锁定库存并重试，禁止提前释放。
- 渠道确认退款成功时执行 `sold_stock -> available_stock`。
- 审核通过只触发渠道退款，不回补库存；审核拒绝、退款失败和退款关闭均不回补库存。

### 3.4 库存不变量

- `available_stock >= 0`；
- `locked_stock >= 0`；
- `sold_stock >= 0`；
- 每个库存业务键最多产生一次有效库存变化；
- 同一订单明细的成功退款数量不得超过购买数量；
- 商品总量不单独存储，管理端按 `available_stock + locked_stock + sold_stock` 展示合计，避免冗余字段漂移。

### 3.5 用户订单创建幂等

- 后端在当前 USER 最终确认下单时即时签发一次性订单幂等键；未使用键的首次提交窗口为 120 秒。
- 所有可能创建用户订单的 POST 请求必须携带后端签发的 `Idempotency-Key` 请求头。
- React/Vue 不生成键，只在真正下单、浏览器重发、Axios 重试和 Nginx 重试时原样带回后端签发值。
- 幂等范围是当前用户，同一用户的相同键只能对应一个订单创建结果。
- “一次性”表示一个键只能绑定一个订单意图，不表示首次请求后失效；键一旦完成订单绑定，后续重试不受 120 秒限制。
- 服务端保存规范化请求指纹；相同键和相同指纹返回第一次创建的订单，相同键但不同指纹返回 HTTP 409。
- 未使用且超过 120 秒、属于其他用户或已用于不同请求的键返回 HTTP 409。
- 重复请求不得再次锁库存、生成关单 Outbox 或清空购物车。
- 分布式锁用于减少竞争；数据库唯一约束负责多实例和 Redis 故障时的最终防重。

## 4. 数据模型

所有表结构同步写入 `payment-demo/sql/payment-demo.sql`。

### 4.1 `t_product` 扩展

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| status | varchar(16) | NOT NULL，`ON_SHELF/OFF_SHELF` |
| available_stock | int unsigned | NOT NULL DEFAULT 0 |
| locked_stock | int unsigned | NOT NULL DEFAULT 0 |
| sold_stock | int unsigned | NOT NULL DEFAULT 0 |
| version | int | NOT NULL DEFAULT 0，乐观锁 |

初始化脚本中的四个演示商品设为 `ON_SHELF`，每个 `available_stock = 100`、`locked_stock = 0`、`sold_stock = 0`。

### 4.2 新增 `t_order_idempotency`

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint unsigned | PK，自增 |
| user_id | bigint unsigned | NOT NULL，签发目标用户 |
| idempotency_key | char(36) | NOT NULL，后端生成 UUID，全局唯一 |
| request_fingerprint | char(64) | 可空，首次下单时写入规范化请求 SHA-256 |
| order_id | bigint unsigned | 可空，成功后关联唯一订单 |
| status | varchar(16) | NOT NULL，`ISSUED/COMPLETED/EXPIRED` |
| expires_at | datetime | NOT NULL，默认签发后 120 秒；仅约束 `ISSUED` 状态 |
| completed_at | datetime | 可空 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

唯一约束：`uk_order_idempotency_key(idempotency_key)`、`uk_order_idempotency_order(order_id)`；索引：`idx_order_idempotency_user_status(user_id, status, expires_at)`。未使用的过期记录可由定时清理任务在保留 7 天后删除；`COMPLETED` 记录忽略 `expires_at`，与关联订单的保留周期一致，不得按未使用键清理。

### 4.3 `t_order_info` 扩展

现有 `checkout_request_id` 保存后端签发并由客户端回传的订单幂等键，扩展覆盖所有用户订单创建入口。保留 `uk_order_user_checkout(user_id, checkout_request_id)` 唯一约束；历史或非用户订单允许为空。

请求指纹只保存在 `t_order_idempotency`。购物车结算指纹至少包含入口类型与支付应用；旧直购指纹至少包含入口类型、商品 ID、数量和支付应用。

### 4.4 `t_order_item` 扩展

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| inventory_status | varchar(16) | NOT NULL，`RESERVED/SOLD/RELEASED` |
| refunded_quantity | int unsigned | NOT NULL DEFAULT 0，累计成功退款数量 |

支付按整张订单完成，因此每个订单明细只会从 `RESERVED` 转为 `SOLD` 或 `RELEASED`。部分退款只增加 `refunded_quantity`，不改变 `inventory_status=SOLD`。

### 4.5 `t_refund_info` 扩展

新增 `application_request_id varchar(64) NOT NULL`，并增加唯一约束：

```text
uk_refund_order_request(order_no, application_request_id)
```

同一请求号和相同请求内容返回第一次创建的退款申请；同一请求号携带不同内容返回 HTTP 409。

### 4.6 新增 `t_refund_item`

| 字段 | 类型 | 约束/说明 |
|---|---|---|
| id | bigint unsigned | PK，自增 |
| refund_id | bigint unsigned | NOT NULL，关联 `t_refund_info.id` |
| order_item_id | bigint unsigned | NOT NULL，关联 `t_order_item.id` |
| product_id | bigint | NOT NULL |
| product_title | varchar(256) | NOT NULL，退款快照 |
| unit_price | int | NOT NULL，订单单价快照（分） |
| quantity | int unsigned | NOT NULL |
| refund_amount | int | NOT NULL，分 |
| create_time | datetime | 创建时间 |

唯一约束：`uk_refund_item(refund_id, order_item_id)`。

### 4.7 新增 `t_inventory_operation`

库存流水保存：`business_key`、`product_id`、`operation_type`、`order_no`、`refund_no`、可用/锁定/售出库存的变化量及前后值、`operator_id`、`operator_name`、`reason`、时间。

`business_key` 全局唯一。操作类型：

- `ADMIN_ADJUST`
- `ORDER_RESERVE`
- `ORDER_COMMIT`
- `ORDER_RELEASE`
- `REFUND_RESTORE`

业务键格式：

```text
ADMIN_ADJUST:{requestId}:{productId}
ORDER_RESERVE:{orderNo}:{productId}
ORDER_COMMIT:{orderNo}:{productId}
ORDER_RELEASE:{orderNo}:{productId}
REFUND_RESTORE:{refundNo}:{orderItemId}
```

### 4.8 新增 `t_message_outbox`

保存 `event_id`、唯一 `event_key`、聚合类型/标识、事件类型、JSON payload、状态、重试次数、下次重试时间、锁持有者、锁过期时间、最后错误和创建/更新时间。

状态：`NEW/SENDING/SENT/FAILED`。发送端通过状态条件更新认领消息，收到 RabbitMQ 发布确认后才标记 `SENT`；进程在发送后、标记前崩溃时允许重复投递。

### 4.9 新增 `t_message_consume_log`

保存 `event_id`、`consumer_name`、事件类型、业务键、状态、每次认领唯一租约 token、锁过期时间、最后错误和消费完成时间，并对 `(event_id, consumer_name)` 建立唯一约束。状态为 `PROCESSING/CONSUMED/FAILED`：首次投递原子插入 `PROCESSING` 并获得短租约；同一事件的活动租约返回 `BUSY`，`CONSUMED` 返回已完成，失败记录或过期租约才允许重新认领并生成新 token。旧执行只能使用自己取得的 token 提交完成/失败，不能覆盖后续租约。远程渠道调用在数据库事务外执行，成功后的本地状态变化、后续 Outbox 和 `CONSUMED` 标记在同一短事务提交；远程成功但本地提交前崩溃时允许重试，并由固定渠道业务号保证最终幂等。Rabbit 消费遇到 `BUSY` 必须拒绝确认以便稍后重试；HTTP 渠道回调遇到 `BUSY` 必须返回失败，只有 `CONSUMED` 才可直接返回成功。

渠道事件统一生成稳定事件标识：微信 V3 优先使用通知 ID，微信 V2 使用交易号，支付宝使用交易号与交易状态；主动查单使用渠道、订单号、渠道交易号和状态的组合。即使不同入口确认了同一业务结果，库存流水业务键仍会阻止重复变化。

## 5. 事务、消息与幂等设计

### 5.1 结算预占

1. 用户最终确认下单后，前端即时调用签发接口；后端生成 UUID 并保存首次提交窗口为 120 秒的 `ISSUED` 幂等记录。
2. Controller 校验回传的 `Idempotency-Key`，服务端按入口和请求参数计算 `request_fingerprint`。
3. 获取 `payment:order:create:{userId}:{idempotencyKey}` 分布式锁。
4. 事务内按用户和键锁定 `t_order_idempotency`；不存在或属于其他用户时返回 409。
5. `COMPLETED` 记录忽略 `expires_at`：指纹相同时返回关联订单，指纹不同时返回 409。
6. `ISSUED` 且当前时间已超过 `expires_at` 时转为 `EXPIRED` 并返回 409；只有未使用键执行此检查。
7. 有效的 `ISSUED` 记录锁定购物车，并按商品 ID 升序锁定商品行，降低多商品并发死锁风险。
8. 校验商品全部上架、数量合法且可用库存充足。
9. 创建带幂等键的订单与 `RESERVED` 订单明细。
10. 将每个商品的可用库存转入锁定库存，写 `ORDER_RESERVE` 流水。
11. 写唯一 `ORDER_CLOSE_SCHEDULED:{orderNo}` Outbox，清空购物车。
12. 将幂等记录原子更新为 `COMPLETED`，保存指纹、订单 ID 和完成时间后提交。
13. 任一步失败时订单、订单明细、库存、流水、Outbox、购物车和幂等记录状态一起回滚；令牌仍为 `ISSUED`，并可在原 120 秒窗口内使用同一请求重试。

既有按商品 ID 直购入口也必须要求 `Idempotency-Key` 并通过同一订单创建和预占服务，不能绕过库存或订单幂等。

未使用键有效期由 `payment.order.idempotency-key-ttl-seconds` 配置，默认值固定为 `120`。

### 5.2 支付成功实扣

微信 V2、微信 V3、支付宝的支付通知和主动查单统一委托支付完成服务：

1. 在事务外完成验签、商户/应用、订单号和金额校验。
2. 获取统一订单锁。
3. 事务内锁定订单、订单明细和相关商品。
4. 以 `NOTPAY -> SUCCESS` 状态条件更新裁决支付与关单竞态。
5. 将明细 `RESERVED -> SOLD`，执行 `locked_stock -> sold_stock`。
6. 保存支付流水、`ORDER_COMMIT` 库存流水和 Inbox 记录。
7. 已成功处理的重复通知或查询直接返回既有成功结果。

### 5.3 取消和超时释放

Outbox 发布器将关单事件投递到现有 RabbitMQ 延迟队列。消费者获取与支付相同的订单锁，在数据库事务外查询并尝试关闭渠道订单。只有渠道明确未支付或已关闭，才开启短事务执行订单状态条件更新、明细 `RESERVED -> RELEASED` 和 `locked_stock -> available_stock`。

渠道超时、网络失败或状态不明确时不释放库存，消息按退避策略重试。支付和关单竞争时，订单状态条件更新只能允许一个方向成功。

### 5.4 按明细退款

退款请求必须包含稳定 `requestId`、订单号、原因和至少一个 `orderItemId + quantity`：

1. 校验当前 USER 是订单所有者且订单处于允许退款状态。
2. 事务内锁定订单和订单明细。
3. 查询该明细所有待审核、已审核、处理中和成功退款数量。
4. 校验本次数量不超过 `quantity - processingQuantity - refundedQuantity`。
5. 按订单单价快照使用精确整数运算计算每项和总退款金额。
6. 创建 `t_refund_info` 与 `t_refund_item`；重复相同请求返回原退款单。

ADMIN 审核通过时只执行状态条件更新并写唯一 `REFUND_SUBMIT_REQUESTED:{refundNo}` Outbox。消费者在数据库事务外使用固定 `refundNo` 调用渠道；远程成功、本地提交前崩溃时，重复调用仍由渠道退款单号保证幂等。

退款通知、主动查单和状态同步统一委托退款完成服务。首次确认成功时，在一个本地事务内更新退款及订单状态、增加 `refunded_quantity`、执行 `sold_stock -> available_stock`、写 `REFUND_RESTORE` 流水和 Inbox。重复成功事件不再次回补。

### 5.5 Outbox 发布与失败恢复

- 业务状态和待发布事件必须同事务落库。
- 发布器使用状态条件更新认领消息，不依赖 Redis 保证正确性。
- RabbitMQ 发布确认前不把消息标为 `SENT`。
- 使用带上限的指数退避；连续失败达到阈值后标记 `FAILED` 并保留最后错误。
- ADMIN 可通过受保护接口把 `FAILED` 消息重新置为可投递状态。
- Inbox 使用带过期时间的 `PROCESSING` 租约；消费失败标记 `FAILED`，消费者崩溃遗留的租约到期后可重新认领，`CONSUMED` 永不再次执行。
- 消费者只在业务事务提交后 ACK；失败则拒绝确认并重试。
- Redis 和分布式锁只降低竞争，数据库状态条件与唯一约束是最终正确性防线。

## 6. API 契约

### 6.1 商品 API

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/product/list` | 公共 | 只返回上架商品 |
| GET | `/api/admin/products` | ADMIN | 返回全部商品与库存 |
| POST | `/api/admin/products` | ADMIN | 新建商品 |
| PUT | `/api/admin/products/{id}` | ADMIN | 修改名称、价格 |
| PATCH | `/api/admin/products/{id}/status` | ADMIN | 上下架 |
| POST | `/api/admin/products/{id}/stock-adjustments` | ADMIN | 调整可用库存 |
| GET | `/api/admin/products/{id}/stock-operations` | ADMIN | 查询库存流水 |
| POST | `/api/admin/outbox/{eventId}/retry` | ADMIN | 重投失败消息 |

公共商品响应包含：`id`、`title`、`price`、`availableStock`、`saleable`。ADMIN 商品响应额外包含：`status`、`lockedStock`、`soldStock`、`version`、`createTime`、`updateTime`。

### 6.2 购物车与结算响应扩展

购物车项增加：`productStatus`、`availableStock`、`purchasable`、`unavailableReason`。添加和修改数量时后端校验商品上架、库存大于 0 且请求数量不超过当前可用库存；结算再次在商品行锁内校验。

### 6.3 订单创建幂等请求头

USER 在创建订单前调用：

```text
POST /api/order-info/idempotency-keys
```

后端返回 `idempotencyKey` 和 `expiresAt`。签发接口被 Nginx 重试时可能产生未使用键，但不会创建订单；未使用键过期后清理。前端不得在进入结算页时提前申请，必须在用户最终确认后即时申请并立即提交订单。

`POST /api/order-info/checkout` 以及所有仍可创建订单的按商品 ID 直购 POST 接口必须携带：

```http
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

客户端不得自行生成该值。现有购物车请求体中的 `checkoutRequestId` 在过渡期保留，但必须与请求头中的后端签发值相同；React/Vue 统一同时发送两处相同值。

订单请求超时或连接断开后，前端必须优先使用原键重试，即使本地观察到 120 秒已过；服务端若已创建订单，会通过 `COMPLETED` 映射返回原订单。只有服务端明确返回“未使用键已过期”时，前端才能申请新键重新提交。

### 6.4 新退款申请 API

```json
POST /api/refund-info/apply
{
  "requestId": "uuid",
  "orderNo": "订单号",
  "reason": "退款原因",
  "items": [
    { "orderItemId": 1, "quantity": 2 }
  ]
}
```

订单明细响应增加：`refundedQuantity`、`processingRefundQuantity`、`refundableQuantity`。退款列表与详情返回对应退款商品、数量、单价快照和计算金额。

保留现有 ADMIN 审核、拒绝、单笔退款查单和按订单退款状态核对接口。

### 6.5 明确删除的旧退款契约

以下请求不做兼容并从 Controller 映射中删除：

- 接受客户端 `refundAmount` 的旧请求结构；
- `/api/refund-info/apply/{orderNo}/{reason}`；
- `/api/wx-pay/refunds` 及其路径参数版本；
- `/api/ali-pay/trade/refund` 及其路径参数版本。

旧 URL 返回 404；新 `/api/refund-info/apply` 收到旧结构时返回 400。

### 6.6 HTTP 错误语义

- 400：非法数量、非法状态、缺少原因或请求字段；
- 401：未登录；
- 403：权限不足、ADMIN 尝试购物；
- 404：商品、订单、订单明细、退款单或旧接口不存在；
- 409：库存不足、商品版本冲突、订单幂等键不存在/过期/跨用户/异参复用、相同退款或库存调整请求号参数冲突，或状态竞态失败。

响应体继续使用现有 `R` 的 `code`、`message`、`data` 结构。

## 7. 鉴权设计

- 将当前宽泛匿名放行的 `/api/product/**` 收窄为仅 `/api/product/list`。
- `/api/admin/products/**` 和 `/api/admin/outbox/**` 同时经过登录与 ADMIN 拦截器。
- `/api/order-info/idempotency-keys` 仅允许 USER；ADMIN 调用返回 403，签发的键必须绑定当前用户。
- USER 可以访问购物车、结算、本人订单和按明细退款申请。
- ADMIN 继续遵守 SPEC-009：禁止购物车、结算、商品支付、已有订单支付和退款申请。
- 支付与退款渠道通知继续匿名访问，但必须完成既有签名/验签和金额校验。

## 8. React 与 Vue 设计

两套前端保持相同路由能力、字段、状态文案、权限和错误行为。

### 8.1 ADMIN 商品管理

- Header 增加“商品管理”；新增 ADMIN 路由 `/products`。
- 表格展示名称、价格、状态、可用库存、锁定库存、净售出库存和更新时间。
- 支持新增、编辑、上架/下架、库存调整和库存流水查看。
- 库存调整弹窗必须填写原因；前端生成 `requestId`，失败重试复用同一值。
- 不显示删除按钮。
- 409 时展示后端原因并刷新最新商品数据。

### 8.2 USER 商品与购物车

- 商品首页只展示上架商品和精确可用库存。
- 可用库存为 0 时显示“已售罄”并禁用加入购物车。
- 用户最终确认结算或旧直购时才向后端即时申请一次性键并立即下单；按钮防重复点击，自动重试和 Nginx 重试均复用后端返回的 `Idempotency-Key`。
- 购物车不可购行显示明确原因；允许移除或调低数量，禁止结算。
- 结算返回库存 409 时刷新购物车和商品数据，不保留半成品订单。

### 8.3 USER 与 ADMIN 退款

- USER 退款弹窗改为勾选订单明细和设置退款数量，不再输入金额。
- 页面展示订单价格快照计算的金额预览，后端金额为最终结果。
- USER 只能选择后端返回的 `refundableQuantity`。
- ADMIN 审核页展示退款商品、数量、计算金额和异步渠道状态。
- 审批通过后库存由渠道成功事件自动回补，页面不提供人工回补按钮。

React 延续 Ant Design，Vue 延续 Element UI，不进行无关视觉重构。

## 9. 部署与兼容

- 本变更以 v0.5.0 发布，旧按金额退款请求和渠道专用退款申请 URL 是明确的破坏性变更。
- `payment-demo/sql/payment-demo.sql` 继续作为唯一完整数据库结构源，不引入 Flyway 或其他迁移框架。
- 应用不会自动执行建表或升级 DDL；本地演示环境升级前必须备份数据，再由维护者明确执行更新后的初始化脚本。
- 原初始化脚本会重建表结构，因此不承诺存量生产数据的原地无损迁移；生产级在线迁移不在本期范围内。
- React 与 Vue 必须和后端 v0.5.0 同步部署，避免旧退款页面调用已删除契约。
- 支付和退款渠道通知 URL、签名协议、支付订单号、退款单号以及现有 `R` 响应外壳保持不变。

## 10. 测试设计

### 10.1 基线与特征测试

- 开工前基线：后端 135 项测试全绿。
- 保持现有支付、订单、认证、管理员边界、消息和对账测试全绿。
- 新增旧退款 URL 返回 404、旧请求结构返回 400 的兼容变更测试。

### 10.2 单元测试

- 商品新增、编辑版本冲突、上下架、库存正负调整与请求幂等；
- 下架商品目录不可见，零库存和超量加购返回 409；
- 结算预占成功、库存不足、重复结算、任一步异常全部回滚；
- 后端订单幂等键签发、用户绑定、过期、成功绑定订单和过期记录清理；
- 后端签发键只能由所属用户在有效期内使用，失败回滚后可重试，成功后绑定唯一订单；
- 未使用键在签发后 120 秒过期，已绑定订单的 `COMPLETED` 键超过 120 秒仍返回原订单；
- 相同订单幂等键重复请求返回同一订单，不同指纹返回 409；
- 支付成功实扣、重复支付通知、主动查单重复同步；
- 取消/超时释放、渠道状态不明不释放、支付与关单竞态；
- 按明细退款金额计算、可退数量、重复申请、并发部分退款；
- 审核通过不回补，退款成功回补，重复退款成功事件不重复回补；
- Outbox 认领、发布确认、退避、失败重投和 Inbox 去重。

### 10.3 集成与并发测试

- 使用真实 MySQL 验证行锁、唯一约束、事务回滚和库存字段非负。
- 初始库存 10，至少 50 个并发结算请求，成功锁定总量必须恰好为 10。
- 至少 20 个相同用户、相同 `Idempotency-Key` 的并发订单请求只能创建一张订单、一组订单明细、一次库存预占和一条关单 Outbox。
- 并发支付与关单只能产生一个最终订单状态和一次库存转换。
- 重复 RabbitMQ 消息只能产生一条相同业务键的有效库存流水。
- 并发退款申请的总数量不得超过订单明细购买数量。
- RabbitMQ 测试验证 Outbox 重投、消费者失败重试和 Inbox 去重。

### 10.4 前端与全量验证

- React `npm run build`；
- Vue `npm run lint`、`npm run build`；
- 浏览器逐项验收 USER 商品、购物车、结算、退款和 ADMIN 商品管理、退款审核；
- 后端执行 `mvn test` 与 `mvn package`。

## 11. 完成标准

- [ ] `available_stock`、`locked_stock`、`sold_stock` 在所有路径下均不为负；
- [ ] 商品库存为 0 时前端禁用加购，后端直接调用也返回 409；
- [ ] 浏览器重复点击、Axios/Nginx 重试和多实例并发不会为一次用户下单意图创建多张订单；
- [ ] 订单幂等键由后端签发并绑定用户，前端自造、跨用户、过期或异参复用均被拒绝；
- [ ] 下架商品不出现在普通商品目录，且不能加购或新建订单；
- [ ] 订单创建锁库存，支付成功实扣，取消/确认关闭释放；
- [ ] ADMIN 审核通过不回补，渠道退款成功才按明细数量回补；
- [ ] 旧按金额退款路径和请求契约已删除；
- [ ] 每次库存变化均有可追溯流水和唯一业务键；
- [ ] Outbox/Inbox 能承受重复投递、消费者重启和本地事务失败；
- [ ] 并发测试证明库存不超卖、退款不超量、消息不重复改变库存；
- [ ] React、Vue 功能和权限一致，构建及浏览器验收通过；
- [ ] spec 实现后从 `planned` 迁移至 `implemented`，README 与测试映射同步更新。

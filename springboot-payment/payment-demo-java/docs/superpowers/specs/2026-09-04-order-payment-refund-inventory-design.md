# 订单、支付、退款与库存统一业务设计

## 1. 基线、版本与实施状态

- 对应 SPEC-010，目标版本调整为 v0.6.0，状态 planned；属于 type: compatibility，并包含数据流设计变更。
- 唯一业务基线：用户指定的 `D:/temp/订单支付退款库存完整业务设计方案.md`（3654 行，SHA-256 `836F7A4311F28C0CF7EE3AB3D41F2D849AEC4FD790E3DFA95AAE2F3E6707E375`）。来源文档保留不改；本文件只负责把其语义映射到当前项目，并补齐用户已明确确认且不与其冲突的项目约束。
- 优先级：来源文档的业务规则 > 本文件的项目映射 > 旧 SPEC/旧代码。旧设计、旧接口或旧测试与来源文档冲突时必须废弃或改写，不能以“保持现状”为由继续生效。
- 本次确认采用独立支付尝试、独立库存预占、资金退款与实物回库分离。旧版“所有普通退款成功才补库存”由第 7 节的类型化规则替代。
- 已有商品管理、库存基础、订单幂等和消息基础设施可复用；不表示新模型已实现。2026-09-05 改造前基线为 Maven 320 项测试全绿，后续每一步用新增红测推动，不能用旧测试全绿冒充 SPEC-010 已完成。
- 使用用户指定的 master 工作区，保留既有修改；本设计不授权执行数据库迁移、清库、提交或发布。

### 1.1 当前实现差异与裁决

| 主题 | 当前实现 | 权威设计/本次裁决 |
|---|---|---|
| 库存事实 | `t_product` 同时保存 available/locked/sold，订单明细只有汇总库存状态 | 建立默认 SKU、`t_sku_stock`、每明细独立 Reservation 和库存流水；切换后旧库存列停写，不双写 |
| 库存扣减 | 旧服务按商品直接预占/提交 | 下单原子 reserve；支付成功才把 Reservation `LOCKED -> COMMITTED` 并扣 locked；关单只释放仍为 LOCKED 的本订单预占 |
| 支付模型 | `t_payment_info` 用 `(order_no,payment_type)` 唯一，订单只能表达一个同渠道付款 | Order 1:N PaymentOrder，所有真实到账分别保留，仅一个 `effective_payment_no`；重复/迟到付款只退对应资金 |
| 支付关单 | 单一订单状态，不能完整表达创建中/未知支付与关单竞态 | 拆分订单、支付、履约、退款状态；先进入 CLOSING，逐支付单查关，确认不可付款后才释放库存 |
| 退款申请 | `RefundRequest.refundAmount` 由浏览器提交，服务端按订单金额创建 | 删除旧按金额申请契约；USER 只提交订单明细和数量，服务端按成交快照计算并同时冻结订单、明细、PaymentOrder 三层额度 |
| 退款与回库 | 旧流程以退款状态直接推动统一库存变化 | 资金与实物独立；批准只排队。未发货取消由不可逆取消证据授权回库，退货由逐批验收可售数量授权，仅退款/差价/重复/迟到不回库 |
| 回库上限冲突 | 来源第 38 节允许资金到账前回库，第 42 节示例却用 `restocked_qty <= refunded_qty` | 采用来源的上位不变量“资金与实物分离”：增加 `restock_authorized_qty`，回库不依赖 `refunded_qty`；流水和批次唯一键保证幂等 |
| 消息幂等 | 有 Outbox/消费日志，但部分回调曾使用 Redis 早成功或缺统一租约语义 | 数据库 Inbox/Outbox 是正确性边界；Redis 仅优化。业务状态与 Inbox 完成同事务，BUSY/失败不向渠道谎报成功 |
| 下单幂等 | 已有后端签发键 | 保留用户已确认的项目映射：后端签发、USER 绑定、首次使用 120 秒；成功绑定后重试始终返回原订单 |
| 管理员边界 | 已有 ADMIN 禁购和部分运维页面 | 保留并覆盖新入口：ADMIN 不购物，只做商品/库存、审批、发货验收、支付/退款状态核对和失败消息重投 |
| 双前端 | React/Vue 仍按金额申请退款，且尚未展示多支付与实物进度 | 两端同步使用同一接口：数量退款、资金/实物分态、多支付单、商品与履约管理；删除友情链接及版权行 |

来源文档未定义项目专有的 120 秒签发键、ADMIN 禁购、两套前端以及“支付成功但本地预占不足时整单退款”的细节；这些是用户此前明确确认的兼容扩展。它们不得削弱来源文档的库存、支付、退款不变量。表名前缀沿用项目 `t_`，语义对应来源文档的 `tb_`，不是另建一套模型。

## 2. 范围与分解

本期实现：商品上下架、库存调整与流水；业务订单与多次支付尝试；后端订单幂等键；库存预占/实扣/释放；商品数量退款、仅退款、差价退款；最小发货/取消履约/退货验收记录；重复付款、迟到付款和库存不足自动退款；Outbox/Inbox；支付单维度对账；React/Vue 同步。

保留 t_product 商品管理入口，每个现有商品迁移出一个默认 SKU。数据库按 SKU 管库存，但本期不增加多规格编辑器、多仓调拨、物流平台接入、促销或优惠券引擎。运费和优惠字段保存服务端成交快照，当前无定价来源时取 0。仓储功能限 ADMIN 记录发货及退货验收，不引入新角色体系。

实施分为四个可验证部分：
1. 回调 Inbox 与现有构建恢复；
2. 订单/支付尝试/库存预占及异常付款；
3. 退款额度/履约取消/退货回库；
4. 两套前端、历史数据与对账适配、集成验收。

## 3. 不变量与权限

1. 购物车不预占库存；移除购物车行不改库存。下架或零库存行可移除。
2. 仅 ON_SHELF 商品进入普通目录；零库存可以展示但不能加购。加购和数量修改校验可用库存，结算再次原子校验。
3. 商品下架不影响已成功预占订单在原期限内支付。ADMIN 调整仅改变可用库存，需原因和稳定 requestId；上下架不改库存。
4. 一个业务订单仅预占一次；新增支付尝试不创建业务订单、不重新预占。
5. 一张订单最多有一个有效成交支付；多笔真实付款均须保留，不得当重复回调吞掉。
6. 可用库存和锁定库存不得为负；正常实扣前整单预检，全部变化同事务。
7. 商品售后退款的已退金额＋冻结金额不超过有效实付，明细已退数量＋冻结数量不超过购买数量。
8. 每笔支付的已退金额＋冻结金额不超过该笔实际到账金额。
9. 回库量不超过有证据授权的可回库量；不使用已退款数量作为回库上限。
10. 渠道结果未知时不释放库存或退款额度。远程调用不进入数据库事务。
11. 每次资金、库存及履约裁决均可追溯、可幂等重试。
12. ADMIN 不得加购、结算、创建购买支付尝试或为自己申请商品退款。管理员仍可运营商品、审批、发货、验收和查询状态。USER 仅操作本人订单。
13. 删除两套前端完整友情链接区块及版权声明行；管理员不展示课程购买入口。

## 4. 数据模型与事实来源

沿用 t_ 表名和项目实体/Mapper/Service 组织方式，避免新旧两套业务订单并存。金额使用 BIGINT 分和 Java long 精确运算；公开 JSON 数字限制在 0..9007199254740991，累加/乘法越界拒绝。库存和数量限制在 Java int 正范围。

| 表/对象 | 关键字段与职责 |
|---|---|
| t_product / Product | 商品名称、价格、ON_SHELF/OFF_SHELF、version；保留管理 API |
| t_product_sku / ProductSku | id、product_id、sku_code、名称/规格快照来源；本期 product_id 唯一，创建唯一默认 SKU |
| t_sku_stock / SkuStock | sku_id 主键、available_stock、locked_stock、version；唯一在线库存事实源 |
| t_order_info / OrderInfo | order_no、user_id、goods_amount、discount_amount、freight_amount、payable_amount、paid_amount、refund_frozen_amount、refunded_amount、freight_refund_frozen_amount、freight_refunded_amount、effective_payment_no、order_status、pay_status、fulfillment_status、refund_status、close_reason、expires_at、close_requested_at |
| t_order_item / OrderItem | order_id、product_id、sku_id、名称/规格快照、quantity、original_amount、discount_amount、pay_amount、refund_frozen_qty/amount、refunded_qty/amount、shipped_qty、cancel_confirmed_qty、received_qty、restocked_qty |
| t_payment_order / PaymentOrder | payment_no、order_no、payment_app_id、channel、protocol、channel_merchant_order_no、channel_transaction_id、request_amount、paid_amount、currency、status、disposition、request_id、request_fingerprint、payment_params、expires_at、paid_at、refund_frozen_amount、refunded_amount |
| t_payment_info / PaymentInfo | 既有真实到账流水，补 payment_no、payment_app_id；保留通知原文与渠道交易号供审计，不承担支付尝试生命周期 |
| t_inventory_reservation / InventoryReservation | reservation_no、order_no、order_item_id、sku_id、quantity、status、expires_at、committed_at、released_at |
| t_refund_info / RefundInfo | refund_no、order_no、payment_no、refund_source、refund_type、application_request_id、request_fingerprint、system_refund_key、approval_status、refund_status、goods_refund_amount、freight_refund_amount、refund_amount、channel_refund_id、channel_status、reason |
| t_refund_item / RefundItem | refund_id、order_item_id、sku_id、快照、quantity、refund_amount、quota_kind、received_qty、accepted_qty、restock_authorized_qty、restocked_qty |
| t_return_receipt / ReturnReceipt | refund_id、refund_item_id、receipt_request_id、received_qty、saleable_qty、rejected_qty、原因、操作人；逐批验收证据 |
| t_fulfillment_operation / FulfillmentOperation | order_no、order_item_id、request_id、operation_type=SHIP/RECEIVE/CANCEL、quantity、操作人及前后数量；唯一 (order_no,request_id,order_item_id,operation_type) |
| t_inventory_operation / InventoryOperation | 唯一 business_key、sku_id/product_id、订单/退款/验收批次、库存变化量及前后值、原因、操作人 |
| t_order_idempotency | 保留 ISSUED/COMPLETED/EXPIRED、用户绑定、指纹、订单关联和 120 秒首次提交窗口 |
| t_message_outbox / t_message_consume_log | 保留发布确认、可恢复租约、幂等事件和失败处理 |

必要唯一约束：
- order_no、payment_no、refund_no、reservation_no 各自全局唯一。
- PaymentOrder: (order_no, request_id)、(payment_app_id, channel_merchant_order_no)、非空 (payment_app_id, channel_transaction_id)。
- Order.effective_payment_no 唯一且只能引用本订单支付；不设置“每单只能一条渠道 SUCCESS”的约束。
- PaymentInfo: payment_no 和非空 (payment_app_id, transaction_id)；移除原 (order_no, payment_type) 唯一约束。
- Reservation: order_item_id 唯一；RefundItem: (refund_id, order_item_id) 唯一。
- 用户 RefundInfo: (order_no, application_request_id)；系统退款填充 `system_refund_key={refundType}:{paymentNo}` 并对该可空列建唯一键，USER 记录必须为 NULL。禁止直接全局唯一 `(payment_no, refund_type)`，否则会阻止同一支付单多次合法部分退款。
- ReturnReceipt: (refund_item_id, receipt_request_id)；库存流水 business_key；Outbox event_key；Inbox (event_id, consumer_name)。
- 同一幂等键必须比较规范化参数；内容冲突返回 409，不吞 DuplicateKeyException。

旧 product.available_stock/locked_stock/sold_stock 和 OrderItem.inventory_status 在迁移核对后停写；线上禁止新旧库存双写。product 管理视图从默认 SKU 取可用/锁定库存，净售出由有效 ORDER_COMMIT 与实物回库流水计算，不再维护 sold_stock 余额。普通预占/释放不计净售出；库存异常释放不冲正常销售。

### 4.1 状态职责

- Order.order_status: WAIT_PAY → CLOSING/ACTIVE；CLOSING → ACTIVE/CLOSED；ACTIVE → COMPLETED。库存不足裁决进入 CLOSED，close_reason=STOCK_SHORTAGE；未付款关单分别记 TIMEOUT/USER_CANCEL。已关闭订单不得重新成交。
- Order.pay_status: UNPAID、PAID、PAID_EXCEPTION。PAID_EXCEPTION 表示有真实付款但无有效成交；paid_amount 只存有效成交金额，capturedAmount/compensationStatus 从支付单和补偿退款聚合展示。
- Order.fulfillment_status: WAIT_SHIP、PART_SHIPPED、SHIPPED、RECEIVED、CANCELLED；按订单明细数量事实汇总，退款不得直接改成未发货。
- Order.refund_status: NONE、REFUNDING、PARTIAL_REFUNDED、FULL_REFUNDED、ABNORMAL，仅汇总商品售后；异常支付退款单独展示，不能把正常成交订单显示成全额退款。
- PaymentOrder.status: CREATED、CREATING、PENDING、UNKNOWN、SUCCESS、CLOSED、FAILED。SUCCESS 保留渠道到账事实，不因退款改写；disposition: NONE、EFFECTIVE、DUPLICATE、LATE、STOCK_SHORTAGE。
- Reservation.status: LOCKED、COMMITTED、RELEASED、ANOMALY_PENDING、ANOMALY。
- RefundInfo: 审核 PENDING/APPROVED/REJECTED；执行 CREATED/PROCESSING/SUCCESS/FAILED/CLOSED/ABNORMAL。FAILED/ABNORMAL 不自动等于可解冻终态；SUCCESS 不得被迟到 PROCESSING 覆盖。
- refund_source: USER/SYSTEM；refund_type 见第 7 节；quota_kind: GOODS/COMPENSATION。库存不足补偿明细保存全量商品快照，但不冒充已售商品售后数量。

### 4.2 锁与事务顺序

业务转换统一先锁 Order，再按主键升序锁 PaymentOrder、RefundInfo、OrderItem/RefundItem、Reservation/Receipt，最后按 sku_id 升序锁 SkuStock；只锁本次需要的子集。ADMIN 纯库存调整仅锁 SKU，不反向获取订单锁。

Redisson 统一订单锁用于降竞争，正确性由上述行锁、条件更新、唯一键和事务保证。Inbox 短事务先认领后提交；业务事务内最后用相同租约 token 标 CONSUMED。complete 条件失败必须回滚全部业务变化；fail 在业务回滚后写入，不覆盖新租约。

### 4.3 金额口径与渠道来源

PaymentOrder.paid_amount 指渠道确认的交易总额（可含渠道优惠），不表示扣手续费后的商户净入账额。订单有效实付、商品退款额度和原路退款上限使用同一交易口径；用户现金额另存 payer_cash_amount，商户实收另存 receipt_amount，未知时为 NULL，不冒充 0。本文件中“全额实际付款/实际到账”均指这一笔已成功交易的总额。

| 渠道 | 校验请求金额及 paid_amount | 单独保存的现金/结算金额 | 退款上限口径 |
|---|---|---|---|
| 微信 V3 | amount.total（分） | amount.payer_total（分） | 原交易 total 减已退/冻结 refund；现金退款 payer_refund 单独展示 |
| 微信 V2 | total_fee（分） | cash_fee、settlement_total_fee（分） | 原交易 total_fee 减已退/冻结退款总额 |
| 支付宝 | total_amount（元用 BigDecimal 精确转分） | buyer_pay_amount、receipt_amount | 原交易 total_amount 减已退/冻结 refund_amount |

不能把现有 PaymentInfo.payerTotal 无条件回填 paid_amount；历史原始通知/查询核实不到对应总额时进入 LEGACY_REVIEW。外部渠道优惠不重写商户已经固化的商品优惠分摊；退款现金/券的渠道分配结果保存原文和独立字段。

口径核对来源：[微信支付通知](https://pay.wechatpay.cn/doc/v3/merchant/4013070368)、[微信 V2 通知](https://pay.wechatpay.cn/doc/v2/merchant/4011935221)、[微信退款申请](https://pay.wechatpay.cn/doc/v3/merchant/4013071036)、[支付宝交易查询](https://aipay.alipay.com/docs/mobile-app-pay/ai-app-pay/alipay-trade-query.html)、[支付宝退款接口](https://aipay.alipay.com/docs/mobile-app-pay/ai-app-pay/alipay-trade-refund.html)。金额字段映射仍须在当前商户产品沙箱中验证，不以文档核对代替真实渠道验收。

## 5. 创建订单、支付尝试与关单

### 5.1 后端 120 秒订单幂等键

保留 POST /api/order-info/idempotency-keys，仅 USER；返回 idempotencyKey/expiresAt。
- ISSUED 键仅在签发后 120 秒内首次使用；先验用户、状态、指纹，再访问购物车/商品。
- COMPLETED 相同指纹永远返回原订单，不因超过 120 秒生成新订单；异参/跨用户/无效键 409。
- 超时重试仍带原键，只有后端明确确认未使用键过期才申请新键。
- 业务订单、明细快照、SKU 预占、Reservation、库存流水、关单 Outbox、购物车清理、COMPLETED 绑定同事务。
- 直购保留原 URL 和键校验，调用相同订单创建服务；不得绕过预占或重用其他键的未支付订单。
- 已绑定键与订单同生命周期，未使用过期键保留 7 天后清理。

### 5.2 支付尝试与号码

用户订单 URL 使用 orderNo；新渠道请求 out_trade_no 使用 paymentNo，独立保存 channel_merchant_order_no。历史支付单允许该值仍是旧 orderNo，不能重写已经提交渠道的号码。

创建尝试使用稳定 requestId（前端生成一次，超时原样重试），唯一范围 (orderNo, requestId)；它不替代后端订单键。不同支付应用/协议或明确“重新支付”使用新 requestId；相同 requestId 返回原 paymentNo，参数不同 409。服务端也复用同应用同协议未到期的 PENDING 尝试；UNKNOWN/CREATING 未确认前只能查询原尝试，不能用新键规避不确定状态。

事务内验证本人、订单 WAIT_PAY、无 close_requested_at、未到期，插入支付尝试后提交。每次发送渠道创建请求前，工作线程必须再次在订单锁内取得 CREATED → CREATING 的发送资格 CAS；关单在相同订单锁下可以原子终结尚未取得发送资格的 CREATED。CREATING/UNKNOWN 表示可能已发送，不能作为“从未创建”释放库存的依据。重试先查询原 paymentNo，只有已核实允许重发且订单未进入关单才再次获取发送资格。

远程创建在事务外，使用固定 paymentNo。创建结果及超时仅条件更新仍处于 CREATING/UNKNOWN 的尝试，不能覆盖回调已写入的 SUCCESS；旧成功创建响应最多补充支付参数。SUCCESS 不被 PENDING/UNKNOWN/CLOSED 或失败结果覆盖；本地 CLOSED/FAILED 后收到已验证成功仍进入统一付款裁决。保存支付参数失败时重试恢复同 paymentNo。

支付过期时间不得晚于业务订单过期时间；剩余小于 60 秒不创建新支付尝试。订单截止时间来自服务端，不由浏览器延长。订单旧 paymentType/paymentAppId/codeUrl 仅兼容投影，不再用于回调、退款或对账路由。

当前实现的业务订单支付窗口与延迟关单时间统一为 5 分钟；120 秒仅是未使用订单幂等键的首次使用有效期，两者不得混用。这样新订单始终存在大于 60 秒的支付尝试创建安全余量。

### 5.3 支付成功与整单库存不足

验签、商户/应用、币种、渠道支付号、交易号及金额核对后，将事件交 PaymentCompletionService；事务内按固化支付单再次校验本地关联。

1. 同一 paymentNo/transactionId 已成功处理：返回原裁决；交易号或金额不同是冲突，不能当重复忽略。
2. 已有另一 effective_payment_no：记录本笔真实到账，disposition=DUPLICATE，创建固定全额补偿退款及 Outbox；不改商品/履约/正常售后额度。
3. Order=CLOSED：记录 LATE 付款并全额退款，不重开订单，不修改 Reservation/商品售后数量。
4. Order=WAIT_PAY/CLOSING 且无有效支付：核验全部明细与 Reservation 完整且状态 LOCKED；商品/SKU 缺失或非法状态是完整性故障，回滚、回调失败并告警，不能虚构正常退款明细。
5. 按 SKU 聚合预检本订单锁定量，在相同 SKU 行锁内检查库存并保持至提交。
6. 全部通过：保存真实付款，设唯一有效支付及 PAID/ACTIVE，将全部 Reservation 转 COMMITTED、扣 locked_stock，写库存流水、ORDER_PAID/关闭其余尝试 Outbox 和 Inbox，同事务提交。
7. 任一聚合 locked_stock 不足：整单无 ORDER_COMMIT。该 SKU 的全部 LOCKED Reservation 转 ANOMALY_PENDING；其余保持 LOCKED。保存实际到账，本次支付标 STOCK_SHORTAGE，订单 CLOSED/STOCK_SHORTAGE、PAID_EXCEPTION，不允许新尝试或履约；创建覆盖全部商品快照和全额实际付款的系统退款，APPROVED/CREATED，写退款提交 Outbox 与 Inbox，同事务提交。
8. 补偿成功落库后回调成功；数据库、补偿或 Outbox 任一步失败都返回失败，由渠道重试和查单恢复。数据完整性失败另记录运维告警，不能把“本地失败”当作“渠道没付钱”。

系统退款业务键使用 AUTO_REFUND:{refundType}:{paymentNo}，存储列至少 varchar(128)；传渠道的 refundNo 独立生成并持久化、重试不变。这样同订单的每笔异常付款各退一次。库存不足订单后续其他真实成功付款标 DUPLICATE，只退款对应资金。

### 5.4 关单与创建支付竞争

超时消息或主动取消先在订单事务内设置 close_requested_at，WAIT_PAY → CLOSING 并写关单 Outbox；这与创建新尝试竞争同一订单行，冻结尝试集合。

远程查询/关闭所有可能发出的尝试，包含各支付应用、V2/V3、CREATING/UNKNOWN。任一成功都进入第 5.3 节；NOTPAY 需继续关渠道单，不能据此立即释放。创建请求在途时的 NOT_FOUND、超时、租约过期都不是安全关闭证据。

全部尝试已确认不可继续付款（渠道 CLOSED，或确证从未发送且不存在在途创建）后，短事务复核订单仍 CLOSING 且无有效支付，更新 CLOSED、LOCKED → RELEASED、locked → available，写唯一 ORDER_RELEASE 流水与 Inbox。未知状态保留锁库并重试/告警；数据库定时扫描到期未终结订单补发稳定关单事件。不能只依赖 MQ 到时一次执行。

## 6. 退款金额、数量与冻结

### 6.1 类型决定占用哪本账

| 类型 | 订单/明细额度 | 原支付单额度 | 库存 |
|---|---|---|---|
| CANCEL_BEFORE_SHIP | 金额＋数量 | 是 | 不可逆取消履约后按取消数量授权 |
| RETURN_AND_REFUND | 金额＋数量 | 是 | 按验收可售数量授权 |
| REFUND_ONLY | 金额＋数量 | 是 | 不回库 |
| PRICE_ADJUSTMENT | 仅金额，quantity=0 | 是 | 不回库 |
| FREIGHT | 仅订单及运费金额，不创建商品退款明细 | 是 | 不回库 |
| DUPLICATE_PAYMENT / LATE_PAYMENT | 不占用、不改变商品已退数量 | 仅该异常支付单 | 不回库 |
| PAYMENT_STOCK_SHORTAGE | 全量 COMPENSATION 快照，不占正常商品额度 | 仅该异常支付单 | 仅第 7.4 节安全释放 |

正常 USER 退款必须关联有效支付且本人拥有订单。系统退款来源、原支付单和金额由服务端确定，用户不能提交。库存不足订单拒绝新的 USER 退款；正常订单仅有部分退款处理中时仍可按剩余额度申请。

### 6.2 精确计算与并发

所有计算在订单锁及相关明细/支付单锁内，以成交快照而非当前商品价格为依据：
- B = floor(pay_amount / quantity)，只从订单行原始成交快照计算，不能因前次退款而改变单件基数。
- M = pay_amount − refunded_amount − refund_frozen_amount。
- Q = quantity − refunded_qty − refund_frozen_qty。
- 数量退款 q 必须 1 ≤ q ≤ Q；q=Q 时退 M，否则 B×q。B×q 必须不超过 M，否则拒绝本次部分数量申请，不能偷偷截断金额；用户可改为退全部剩余数量，由最后一次吸收尾差。
- 所有乘法、加法、扣减均使用 `Math.multiplyExact`/`Math.addExact` 或等价受检运算；任何层级额度条件更新失败时整笔事务回滚。
- 同一订单行的差价退款和数量退款不能同时未决；差价退款拒绝或成功后才能创建数量退款，反向亦然，避免数量全退后释放出不可退余额。
- 差价退款由 ADMIN 通过专用入口输入目标剩余商品成交总额，服务端计算差额；当前 USER 入口仍只接受明细数量，不接受任意 refundAmount。差额必须 >0 且不超过该行未退净额。
- 合法售后计算金额为 0 时仍受数量、审核与履约约束；满足执行条件后本地标结算成功、channel_status=NOT_REQUIRED，不调用渠道，不假冒渠道成功通知。

创建退款的一笔事务同时冻结适用的订单金额、运费金额、明细金额/数量、支付单金额，并插入退款及明细。条件更新任一失败全部回滚；相同 requestId 同内容返回原退款，异参 409。

只有尚未发送渠道请求且未发生不可逆取消/验收入库的待审核申请可以拒绝并原子解冻所有适用层级。退款成功用状态 CAS 原子把全部适用冻结余额转已退余额。渠道超时/未知/可重试失败均保持冻结，使用同 refundNo 查单和重试。履约取消或已回库后资金失败必须继续履行原退款义务，不能解冻后允许重复申请。

### 6.3 优惠、运费与响应

成交时固化每行 original_amount、discount_amount、pay_amount，并满足行金额合计＋运费=订单应付。初期无优惠来源时 discount=0；不信任客户端价格、优惠或运费。

整单未发货取消且全部未退款商品均被该申请覆盖时，服务端纳入尚未退/冻结的原运费；普通部分退款与普通退货默认不退运费。ADMIN 可对原运费做独立受限退款，不能超过 freight_amount，且仍占订单和原支付单余额。额外赔偿、退货运费补贴不走超出原支付金额的渠道退款，本期不实现额外打款。

API 返回商品款、运费、总退款款和各明细金额。数量字段与金额字段分别展示；“已退钱”和“已回库”不得共用一个状态。

## 7. 审核、履约与库存回补

### 7.1 审核仅批准退款

ADMIN approve 只状态 CAS＋REFUND_SUBMIT_REQUESTED Outbox，不远程退款、不直接增加库存。执行消费者检查该退款类型所需履约证据：
- 未发货取消：短事务与发货争夺订单/明细，确认剩余未发货数量可取消，记录 cancel_confirmed_qty 和回库授权，再进入回库/资金处理。
- 退货退款：审批后等待真实验收；累计 received_qty 达到批准退货数量且验收结果已记录后触发资金提交。可售与不可售商品都可以记录验收，只有可售数量能回库；质量不达预期进入人工审核，不自动改退款金额。
- 仅退款、差价退款、系统补偿：不需要实物回库条件。
证据未齐时记录等待原因，不反复重投渠道请求；由履约/验收事件推进。

### 7.2 未发货取消和发货竞争

每行约束 shipped_qty + cancel_confirmed_qty ≤ quantity。ADMIN 发货操作包含稳定 requestId 和明细数量，复用唯一业务记录防重复；与取消确认竞争同一订单行锁，且不能发出已经取消的数量。

取消先成功则永久禁止对应数量发货，事务记录该 RefundItem 的 restock_authorized_qty。发货先成功则拒绝未发货取消；在尚未发送渠道请求/未回库条件下，终结该申请并解冻，不擅自改成退货退款。用户可另行申请退货。

每个取消回库事件仅执行一次。资金尚未到账不阻碍有明确取消证据的可售回库；资金失败仍保留原退款义务。

### 7.3 分批退货与授权回库

ADMIN 每次验收提交 receiptRequestId、refundItemId、receivedQuantity、saleableQuantity、reason；不可售量=收货量−可售量。要求 0≤可售量≤收货量，累计收货量≤批准退货数量。

同 (refundItemId, receiptRequestId) 相同内容返回原结果，异参 409。单次事务更新 Receipt、RefundItem 收货/可售/授权/回库累计、OrderItem.restocked_qty、SkuStock.available_stock、库存流水及后续 Outbox：
- 本明细已回库＋本次量 ≤ 本明细授权可回库量 ≤ 批准售后数量。
- 库存流水键 RETURN_RESTOCK:{receiptRequestId}:{refundItemId}；第二个合法批次可以继续回库。
- 不使用 restocked_qty ≤ refunded_qty 判断合法性；资金和实物完成时间允许不同。
- 不可售商品记录验收证据但不增加可售库存；本期不建立残次品仓储余额。
- 收货和发货证据限制该行累计退回数量不得超过实际已发数量。回库成功不能因退款渠道失败回滚或再次执行。

### 7.4 支付库存异常的安全释放

此类型不是普通退货：商品尚未正常实扣，不执行“已售商品回库”。

只有渠道确认该 STOCK_SHORTAGE 退款成功后才处理对应保留预占：
- 首次预检与退款时均能证明该 SKU 的本订单完整锁定量存在：LOCKED → RELEASED，locked → available。
- ANOMALY_PENDING 不管此刻聚合锁定量多少，都只转 ANOMALY，写零变化 PAYMENT_STOCK_ANOMALY 流水，绝不释放其他订单新锁入的库存。
- 初检通过但退款时再检不足：该 SKU 所有本订单 LOCKED 预占转 ANOMALY，不增加 available。
- 已 RELEASED 保持不变，不再次释放。
- COMPENSATION 明细记录资金补偿完成，不增加正常商品售后 refunded_qty。UI 从补偿退款明细展示全量退回数量。
- 资金成功事实、补偿状态和安全释放/异常流水在同一本地事务提交；退款事务不得被“可证明的库存不足”误判失败，但任意数据库异常仍需回滚重试。

## 8. Outbox、Inbox 与恢复

所有关键业务和必要 Outbox 同事务；时序图中的“COMMIT 后直接发 MQ”统一改为后台发布 Outbox。发布需 correlated confirm、mandatory/returns，收到确认才标 SENT；崩溃允许重复投递。ADMIN 只能重投 FAILED 事件，不能篡改事件内容。

Inbox 使用 PROCESSING 租约、CONSUMED、FAILED 和每次认领 token。BUSY 的 HTTP 回调必须失败，Rabbit 延迟重试；CONSUMED 才可重复成功。禁止微信 V2/V3 Redis SETNX 失败直接回成功。事件冲突先拒绝，不得覆盖已处理记录。

稳定身份：
- 微信 V3 通知优先渠道 id；微信 V2 用交易号；支付宝用商户应用＋交易号＋交易状态。
- 主动支付查询用支付应用＋paymentNo＋交易号＋状态；退款用支付应用＋refundNo＋渠道状态。
- 超长或组合身份转换为名称型 UUID；业务键记录 paymentNo/refundNo，消费者名称区分通知与查询。
- Rabbit 查询 attempt 加入事件键；状态发生变化与尝试次数不同均可推进。FAILED/ABNORMAL/UNKNOWN 是否继续查询取决于是否已明确终结，不可把临时失败当资金已撤销。
- 不确定远程结果使用固定 paymentNo/refundNo 重试；租约超时并不授权换渠道业务号。
- 定时补偿扫描未完成订单、CREATING/UNKNOWN 支付尝试及未终结退款；采用相同事件服务，不另写一套状态更新。

## 9. API 与双前端契约

维持现有 R 外壳和回调 URL/验签/应答格式；v0.6.0 统一部署两套前端与后端。业务订单号不变，新渠道商户支付号使用 paymentNo。

| API | 权限/契约 |
|---|---|
| POST /api/order-info/idempotency-keys | USER，后端签发 120 秒键 |
| POST /api/order-info/checkout | USER，Idempotency-Key；返回 orderNo，不在下单事务调用渠道 |
| POST /api/order-info/{orderNo}/payments | 本人 USER，requestId/paymentAppId/protocol；返回 paymentNo、orderNo、channel、状态、支付参数 |
| GET /api/order-info/{orderNo}/payments | 本人 USER/ADMIN，列表与有效/异常付款标记 |
| POST /api/payment-orders/{paymentNo}/query | 本人 USER/ADMIN，按指定支付单查单并同步 |
| POST /api/order-info/{orderNo}/cancel | 本人 USER，仅未付款关单；已付款走指定商品售后申请 |
| POST /api/refund-info/apply | 本人 USER，requestId/orderNo/refundType/reason/items[{orderItemId,quantity}]；只允许数量类售后 |
| POST /api/admin/refunds/price-adjustments | ADMIN，requestId/orderNo/orderItemId/targetRemainingGoodsAmount/reason；后端计算差额 |
| POST /api/admin/refunds/freight | ADMIN，requestId/orderNo/reason；退尚未退/冻结的原运费，amount 由后端计算 |
| POST /api/order-info/{orderNo}/shipments | ADMIN，requestId/items[{orderItemId,quantity}]/reason |
| POST /api/order-info/{orderNo}/receipt | 本人 USER，requestId/items[{orderItemId,quantity}]；收货量不超过实际发货量，记录 RECEIVE 幂等流水 |
| POST /api/refund-info/{refundNo}/receipts | ADMIN，requestId、验收明细；每项生成对应 Receipt 幂等记录 |
| GET /api/refund-info/{refundNo}/receipts | 本人 USER/ADMIN，验收及回库进度只读 |
| 现有 approve/reject/query/reconcile | 保留 ADMIN 路径并复用统一服务；系统退款禁重复审批/拒绝 |
| /api/admin/products、上下架/库存调整/流水 | 保留商品级入口，映射默认 SKU；不提供商品物理删除 |
| POST /api/admin/outbox/{eventId}/retry | ADMIN，仅失败记录可重投 |

旧渠道直购/已有订单支付 URL 是统一服务的适配入口，也必须带稳定支付 requestId（直购由后端订单键确定首个稳定尝试身份），不得另造订单或绕过鉴权。渠道旧 query 接口通过支付单归属路由；按订单查单需返回或汇总多笔尝试，不默认只查 Order.paymentAppId。

删除旧按金额 RefundRequest 和旧 /api/wx-pay/refunds、/api/ali-pay/trade/refund 申请映射（不含退款通知 URL），以及旧路径参数申请接口。旧金额请求 400，删除路径 404；越权 403/不泄漏他人资源的 404，幂等/数量/库存/状态冲突 409。

React/Vue 同步显示四维订单状态、支付尝试和异常补偿、选择商品数量退款、分开的退款金额/回库数量。ADMIN 商品管理、退款审批、发货、分批验收、支付/退款查询和消息重试共用后端 API；用户看不到资金差价管理入口。前端超时不丢弃原订单键或支付/退款请求号，提交期间禁重复点击只是体验措施。

## 10. 兼容、迁移与对账

1. 初始化 SQL 仅用于明确的新库；现有 payment-demo.sql 含 DROP，禁止用作存量升级。
2. 提供独立增量 SQL、只读核对和迁移说明。先备份并在副本演练；切换窗口冻结所有资金/库存写入者：订单创建、已有订单支付尝试、ADMIN 库存调整、发货/验收/退款、支付/退款查单同步、MQ消费者和补偿扫描。等待在途事务完成并处理任务租约；渠道事件经验签可靠暂存，保存失败须让渠道重试，不可无持久化应答成功。只允许一套应用取得写入/消费资格，迁移验证后由新模型重放。执行线上迁移需要另行授权。
3. 一个旧商品生成默认 SKU，按原 available/locked 的准确余额初始化 SkuStock；一个旧明细映射一个 Reservation。迁移本身不执行 reserve/commit/release，也不生成虚构的新增库存流水。
4. 旧 SOLD/RESERVED/RELEASED 必须与订单和库存流水核对。缺失库存/订单快照、来源不明余额均列差异并阻止该批写入切换，不凭订单总金额推算库存。
5. 旧支付使用真实应用、旧渠道商户订单号、交易号建立 PaymentOrder 映射。可证明唯一正常付款才设有效支付；多笔/冲突/未知来源进入待核对。未知支付号不能退回“把 paymentNo 当 orderNo”的兜底路由。
6. 旧金额退款无法可靠推断商品明细；保留历史资金记录并允许继续查询已提交退款，但不自动生成 RefundItem 或回库。此类订单标 LEGACY_REVIEW，禁止新数量售后/发货/自动库存变化；通过经审核的映射材料完成行金额与数量核对后才能解除。
7. 旧字段仅作迁移/兼容投影。停写后新表是唯一事实源；不双扣、不双加。迁移需要可重复检查和逐阶段校验点；已有新真实付款后不得简单回滚到旧应用，应前向恢复。
8. 支付对账从业务订单维度改为 paymentNo＋支付应用/交易号，退款按 refundNo＋paymentNo。所有正常、重复、迟到、已退款付款都参与，不仅有效支付。账单展示同时给出业务 orderNo。
9. 保留金额分、商户配置、通知原文；对账发现未知渠道退款只记录差异，不凭金额“修复”出可回库商品明细。
10. 本期没有多规格/仓储平台/促销引擎；完整模型字段不意味着这些平台功能已交付。

## 11. 验收与完成定义

以下都完成才能把 SPEC-010 从 planned 标为 implemented：

- [ ] 商品上/下架、库存调整审计、零库存禁止加购、失效购物车行移除、ADMIN 禁购及两端入口一致。
- [ ] 120 秒未使用键过期；已成功绑定键超过 120 秒仍重放原订单；20 个同键并发只创建一单/一次预占。
- [ ] 10 件库存、50 次并发下单最多预占 10 件，无任何负库存。
- [ ] 新支付尝试不锁第二次库存；同渠道第二笔真付款保留并只补偿一次。
- [ ] 支付创建 UNKNOWN 与关单竞态不提前释放；支付成功与确认关单只裁决一个履约结果。
- [ ] 正常支付整单实扣；任一锁定库存不足整单无实扣、全额自动退款；补偿落库失败回滚并让回调失败。
- [ ] 异常预占永不借其他订单后来锁入的库存释放；迟到/重复付款不改变商品退款额度或库存。
- [ ] 20 个并发商品退款总数量/金额不超限；申请/拒绝/成功完整冻结/解冻/结算全部适用层级。
- [ ] 差价与数量退款未决互斥、尾差全退一致、零金额售后不发起渠道退款。
- [ ] 审核不直接回库；未发货取消与发货互斥；退货验收先于资金到账也能合法回库。
- [ ] 同批验收重复不增库，第二批合法验收能入库；不可售商品、仅退款、差价退款不增可售库存。
- [ ] 远程超时保留退款额度；已回库后退款失败仍履行原退款，不允许重复申请侵占额度。
- [ ] Outbox 无确认不 SENT；Inbox BUSY 不成功；旧 token 不能提交；重复/崩溃重投最终只变更一次。
- [ ] 历史样本迁移余额核对通过；新旧商户订单号均正确查单/退款/对账；未知退款不虚构库存。
- [ ] MySQL/RabbitMQ 集成测试、双端构建/页面验收、Maven package 和独立审查通过；既有警告与未验证外部环境明确记录。

# 订单、支付、退款与库存统一模型 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 按 SPEC-010 v0.6.0 实现订单多次支付、可靠库存预占、类型化退款与实物回库，以及支付库存异常整单补偿。

**Architecture:** 保留 Spring Boot 单体及同库事务。业务订单是库存/履约主体，PaymentOrder 保存每次真实渠道尝试，Reservation 保存逐明细库存权利，RefundInfo 按退款类型选择额度和回库规则。远程渠道操作通过稳定业务号、Outbox/Inbox 和短事务恢复。

**Tech Stack:** Spring Boot 2.3.7、MyBatis-Plus、MySQL、Redisson、RabbitMQ、JUnit 5/Mockito；React/Vite/Ant Design；Vue 2/Element UI。

**Working directory:** D:/code/demo/springboot-payment/payment-demo-java。以下文件相对该目录；Maven 命令在 payment-demo，npm 命令在对应前端目录运行。

**Specification:** [统一设计](../specs/2026-09-04-order-payment-refund-inventory-design.md) 与 [SPEC-010](../../../spec/planned/PRODUCT_INVENTORY_MANAGEMENT_COMPAT_SPEC.md)。

**Execution policy:** 使用用户指定 master。每个任务独立红测、实现、绿测及审查；不自动提交、不执行存量库迁移、不覆盖无关修改。旧计划 Task 1–5 只是可复用基础，不能直接勾选下述新任务。每个任务完成后检查 git diff --check；失败先查根因，不为变绿删除业务断言。

## 分段与契约边界

| 分段 | 任务 | 完成后可验证结果 |
|---|---|---|
| A：恢复可靠回调基线 | 1 | 当前源代码与测试可编译；BUSY 不成功，业务与 Inbox 同事务 |
| B：订单/支付/库存 | 2–6 | 一次下单只预占一次，多次支付和关单竞态安全，异常到账可全额补偿 |
| C：售后/实物回库 | 7–9 | 退款金额/数量不超限，实物证据控制回库，渠道未知不丢退款义务 |
| D：客户端/迁移验收 | 10–12 | 双端 API 一致，历史核对及真实基础设施验证完备 |

跨任务公共契约固定为：
- PaymentAttemptService.create(orderNo, requestId, paymentAppId, protocol) → PaymentOrder；
- PaymentCompletionService.complete(VerifiedPaymentEvent) → PaymentCompletionResult；
- InventoryService.reserve(order, items)、preflightAndCommit(orderNo)、releaseReservation(orderNo)、releaseCompensatedReservation(refundNo)；
- RefundApplicationService.createApplication(RefundApplyRequest) → RefundInfo；
- RefundCompletionService.complete(VerifiedRefundEvent)；
- FulfillmentService.ship(orderNo, request)、confirmCancellation(refundNo)、receive(orderNo, request)；
- ReturnReceiptService.record(refundNo, ReturnReceiptRequest)；
- 事件解析/验签在渠道适配器，完成服务只接受已核验事件；完成事务重新核对固化的金额/关联。
- 退款/支付消息不得调用独立的旧更新路径；同一业务状态变化仅有一个完成服务入口。

### Task 1：收尾当前 Inbox 并恢复构建

**Files**
- Modify: payment-demo/src/main/java/cc/ivera/controller/support/WxPayNotifyHandler.java
- Modify: payment-demo/src/main/java/cc/ivera/controller/WxPayV2Controller.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayRefundService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/RefundApplicationServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/mq/InboxEventIds.java、InboxEventTypes.java、RefundStatusSyncConsumer.java
- Modify: payment-demo/src/test/java/cc/ivera/mq/ChannelNotificationInboxTest.java
- Create: payment-demo/src/test/java/cc/ivera/controller/WxPayNotifyHandlerTest.java
- Create: payment-demo/src/test/java/cc/ivera/mq/ChannelQueryInboxTest.java
- Adjust constructor fixtures: PaymentInventoryTransitionTest、OrderCloseInventoryTransitionTest、OrderTransitionConcurrencyTest、InfrastructureBehaviorCharacterizationTest、AdminPurchaseBoundaryTest、ExistingOrderPaymentTest、OrderCreationIdempotencyHeaderMvcTest。

- [x] 写/修正红测：V2/V3/支付宝支付与微信退款 CLAIMED/CONSUMED/BUSY/fail；支付宝 eventId 基于交易号＋状态；回调 HTTP/XML 失败语义。TransactionTemplate callback 中用 insideTransaction=true，complete 断言为 true、fail 断言为 false；不能只验证调用顺序。
- [x] 运行 mvn '-Dtest=ChannelNotificationInboxTest,WxPayNotifyHandlerTest,ChannelQueryInboxTest' test，记录真实失败；当前旧构造器缺参属于已知编译缺口，按新依赖修 fixture，不改业务期望。
- [x] 删除通知 handler 的 Redis processing 即成功分支。核心分支：
  ~~~java
  if (claim.getStatus() == MessageConsumeClaim.Status.CONSUMED) return;
  if (claim.getStatus() == MessageConsumeClaim.Status.BUSY)
      throw new ConflictException("通知正在处理，请重试");
  ~~~
  CLAIMED 的业务写入和 complete 同事务；数据库失败/complete失败回调均失败，事务外 fail。主动查询具有独立消费者身份，远程 HTTP 在事务外，状态变化与 Inbox 同事务。
- [x] 校正退款同步消息：UNKNOWN/可重试失败保留资金冻结并继续查询；查询 attempt 唯一，提交和后续 Outbox/Inbox 接入不影响旧路径特征测试。
- [x] 运行 mvn test；审查新增构造器调用点、事务边界及回调端点。55 项 Inbox 聚焦测试和 320 项全量测试通过，`git diff --check` 通过；此任务未引入新商品退款接口、未改库存不足业务裁决。

### Task 2：新模型表、领域类型与迁移前置校验

**Files**
- Modify: payment-demo/sql/payment-demo.sql
- Create: payment-demo/sql/migrations/20260904_v060_expand.sql
- Create: payment-demo/sql/migrations/20260904_v060_verify.sql
- Create: payment-demo/src/main/java/cc/ivera/entity/ProductSku.java、SkuStock.java、PaymentOrder.java、InventoryReservation.java、RefundItem.java、ReturnReceipt.java、FulfillmentOperation.java
- Modify: payment-demo/src/main/java/cc/ivera/entity/OrderInfo.java、OrderItem.java、PaymentInfo.java、RefundInfo.java、InventoryOperation.java
- Create: payment-demo/src/main/java/cc/ivera/enums/BusinessOrderStatus.java、PaymentOrderStatus.java、PaymentDisposition.java、ReservationStatus.java、RefundType.java
- Create: payment-demo/src/test/java/cc/ivera/schema/UnifiedTradeSchemaTest.java

- [x] 写表契约测试：所有统一设计第 4 节字段/非负/唯一约束；不能有 (order_no,payment_type) 的付款去重约束；增量文件无 DROP/TRUNCATE。
- [x] 运行 mvn '-Dtest=UnifiedTradeSchemaTest' test 验证字段或约束缺失；首次在增量文件缺失处红，补齐后 5 项结构契约全绿。
- [x] 实现增量建表与 nullable 扩展字段，旧列暂不删；订单有效支付唯一引用、每明细一个 Reservation、支付请求/回库批次幂等约束都落 SQL。系统退款使用可空 `system_refund_key={refundType}:{paymentNo}` 唯一键，不能用全局 `(payment_no,refund_type)` 阻塞用户多次部分退款。关键断言：
  ~~~sql
  UNIQUE KEY uk_payment_request (order_no, request_id),
  UNIQUE KEY uk_payment_merchant_no (payment_app_id, channel_merchant_order_no)
  ~~~
  渠道交易号允许尚未知的 NULL，已有值后唯一且不可改成另一个付款。
- [x] 添加类型映射和受检 long 金额运算；公共数字越界、负金额、零/负数量、int 库存加法溢出测试。旧中文 OrderStatus 只作为迁移/投影，业务开始使用新独立状态。
- [ ] 运行 schema 单测；仅在隔离测试库验证 DDL。schema/数值 15 项及后端全量 328 项已通过；Docker CLI 存在但 Engine 未运行，尚未在隔离 MySQL 执行 DDL。存量生产库不执行；付款/库存证据异常由 verify SQL 输出差异并阻断切换。

### Task 3：默认 SKU 与独立预占库存

**Files**
- Create: payment-demo/src/main/java/cc/ivera/mapper/ProductSkuMapper.java、SkuStockMapper.java、InventoryReservationMapper.java
- Create: payment-demo/src/main/resources/mapper/SkuStockMapper.xml、InventoryReservationMapper.xml
- Modify: payment-demo/src/main/java/cc/ivera/service/InventoryService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/InventoryServiceImpl.java、ProductAdminServiceImpl.java、ProductServiceImpl.java、CartServiceImpl.java、CheckoutServiceImpl.java、OrderInfoServiceImpl.java
- Create: payment-demo/src/test/java/cc/ivera/service/impl/InventoryReservationTest.java

- [x] 写正常/不足/重复预占、整单预检、已释放重复释放、管理员调整只动 available、商品下架与已有订单支付的红测。
- [x] 运行 mvn '-Dtest=InventoryReservationTest,InventoryServiceTest,ProductAdminServiceTest,CartServiceImplTest' test；扩展聚焦集共 78 项通过。
- [x] 新商品同事务创建默认 SKU；商品/购物车 API 映射默认 SKU 库存。预占 SQL：
  ~~~sql
  UPDATE t_sku_stock
  SET available_stock = available_stock - #{qty},
      locked_stock = locked_stock + #{qty}, version = version + 1
  WHERE sku_id = #{skuId} AND available_stock >= #{qty};
  ~~~
  每条明细落独立 Reservation，流水业务键包含 reservationNo；新旧库存禁止双写。
- [x] 整单按 SKU 升序锁定并先完成所有检查，再更新任一余额。commit/release 与 Reservation 状态 CAS、库存流水同事务；异常 SKU 不可用其他订单锁定量补偿。
- [x] 运行库存/商品/购物车回归；后端全量 335 项通过，120 秒订单键及同键重放测试保持全绿。真实数据库并发验证仍由 Task 12 执行。

### Task 4：独立 PaymentOrder 与现有渠道接口适配

**Files**
- Create: payment-demo/src/main/java/cc/ivera/mapper/PaymentOrderMapper.java
- Create: payment-demo/src/main/resources/mapper/PaymentOrderMapper.xml
- Create: payment-demo/src/main/java/cc/ivera/service/payment/PaymentAttemptService.java、PaymentAttemptServiceImpl.java
- Create: payment-demo/src/main/java/cc/ivera/dto/PaymentAttemptRequest.java
- Create: payment-demo/src/main/java/cc/ivera/controller/PaymentOrderController.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/controller/WxPayController.java、WxPayV2Controller.java、AliPayController.java
- Create: payment-demo/src/test/java/cc/ivera/service/payment/PaymentAttemptServiceTest.java

- [ ] 红测：相同 requestId 重试同 paymentNo；异参冲突；换应用新尝试不锁库存；UNKNOWN 不换号；close_requested_at/不足60秒/ADMIN禁止创建。
- [ ] 运行 mvn '-Dtest=PaymentAttemptServiceTest,ExistingOrderPaymentTest,AdminPurchaseBoundaryTest' test。
- [ ] 实现订单锁内建立尝试与请求指纹；发送前 CREATED→CREATING 资格 CAS 与关单竞争相同订单锁，提交后才调渠道。wechat/alipay out_trade_no=固化 channelMerchantOrderNo，新单等于 paymentNo。创建响应/超时仅条件回写，不覆盖已成功付款；回写失败仍查同一尝试恢复。
- [ ] 回调/查询/退款路由按 PaymentOrder 固化应用与协议；用户 URL 仍业务 orderNo。旧直购接口先通过后端订单键创建/重放业务单，再创建稳定首个支付尝试。
- [ ] 验证用户所有权、商户/应用/金额/币种校验、远程调用不在事务内；渠道创建超时不可当 CLOSED。

### Task 5：统一支付完成、关单与异常资金补偿

**Files**
- Create: payment-demo/src/main/java/cc/ivera/service/payment/VerifiedPaymentEvent.java、PaymentCompletionResult.java、PaymentCompletionService.java、PaymentCompletionServiceImpl.java、OrderCloseService.java
- Create: payment-demo/src/main/java/cc/ivera/service/refund/AutomaticRefundService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/PaymentInfoServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/mq/OrderCloseConsumer.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayOrderService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/controller/WxPayV2Controller.java
- Create: payment-demo/src/test/java/cc/ivera/service/payment/PaymentCompletionServiceTest.java、PaymentCloseRaceTest.java、StockShortageRefundTest.java

- [ ] 红测：同一付款重复；第二筆同/异渠道付款；支付与关单竞态；CREATING/UNKNOWN/NOTPAY/NOT_FOUND不能提前释放；任何子项库存不足整单不扣；完整性异常与库存不足区别。
- [ ] 运行 mvn '-Dtest=PaymentCompletionServiceTest,PaymentCloseRaceTest,StockShortageRefundTest' test。
- [ ] 完成服务锁订单→支付单→明细/Reservation→SKU，先核验付款与整单库存；成功则裁决 effective_payment_no。第二笔真付款保存独立流水；补偿单唯一键 (payment_no,refund_type)，不得依赖旧 OrderStatus 非 NOTPAY 直接返回。
- [ ] 库存不足事务原子保存付款、全量 COMPENSATION 明细、ANOMALY_PENDING、CLOSED/STOCK_SHORTAGE、退款 Outbox、Inbox；任何失败回滚并回调失败。关单先设置 CLOSING 门闩，再远程逐笔查关，所有尝试安全终结后短事务释放。
- [ ] 运行并发单测，断言每张业务订单只有一次有效库存实扣，所有实际付款都有流水与归属；不同异常 paymentNo 均有各自全额补偿。

### Task 6：支付单查询、对账与失败恢复

**Files**
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/reconciliation/ReconciliationServiceImpl.java、WxPaymentRefundMatcher.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayRefundService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java
- Create: payment-demo/src/main/java/cc/ivera/task/TradeRecoveryTask.java
- Modify: payment-demo/src/main/java/cc/ivera/mq/OutboxEventTypes.java
- Create: payment-demo/src/test/java/cc/ivera/service/payment/PaymentReconciliationMappingTest.java、TradeRecoveryTest.java

- [ ] 红测：同单两笔同渠道金额相同也分别对账；新商户单号paymentNo与历史orderNo都能准确映射；未知退款只报差异；CLOSING/UNKNOWN持久恢复。
- [ ] 运行 mvn '-Dtest=PaymentReconciliationMappingTest,TradeRecoveryTest,WxPaymentRefundMatcherTest' test。
- [ ] 将应用筛选、支付匹配和退款原路关联改为 PaymentOrder；异常付款也对账。原 orderNo 作为用户业务维度展示，不作为唯一流水索引。
- [ ] 定时扫描未完成订单/支付/退款，调用现有稳定事件写入服务；扫描不直接扣库存。消息以固定业务号重放，退避和错误明细可见。
- [ ] 验证扫描与回调并发不改变有效付款归属、不重复退钱；不能把租约到期当渠道已关闭证明。

### Task 7：类型化退款申请、额度与旧接口删除

**Files**
- Create: payment-demo/src/main/java/cc/ivera/dto/RefundApplyRequest.java、RefundItemRequest.java、PriceAdjustmentRequest.java
- Create: payment-demo/src/main/java/cc/ivera/mapper/RefundItemMapper.java
- Create: payment-demo/src/main/resources/mapper/RefundItemMapper.xml
- Modify: payment-demo/src/main/java/cc/ivera/service/RefundApplicationService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/RefundApplicationServiceImpl.java、RefundInfoServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/controller/RefundApplicationController.java、RefundInfoController.java
- Delete obsolete request after replacing usages: payment-demo/src/main/java/cc/ivera/dto/RefundRequest.java
- Create: payment-demo/src/test/java/cc/ivera/service/refund/RefundQuotaTest.java、RefundAllocationTest.java
- Modify: payment-demo/src/test/java/cc/ivera/controller/RefundOwnershipTest.java
- Modify: payment-demo/src/test/java/cc/ivera/characterization/PublicApiCharacterizationTest.java

- [ ] 红测：归属/重复item/数量上限、冻结三层额度、异参幂等、差价与数量退款互斥、10000分/3件尾差、差价后重算、零金额售后、原运费上限及异常资金不占商品额度。
- [ ] 运行 mvn '-Dtest=RefundQuotaTest,RefundAllocationTest,RefundOwnershipTest' test。
- [ ] 实现锁内精确计算：
  ~~~java
  long baseRefundAmount = item.getPayAmount() / item.getQuantity();
  long remainingAmount = item.getPayAmount() - item.getRefundedAmount() - item.getRefundFrozenAmount();
  int remainingQuantity = item.getQuantity() - item.getRefundedQty() - item.getRefundFrozenQty();
  long amount = quantity == remainingQuantity
          ? remainingAmount : Math.multiplyExact(baseRefundAmount, quantity);
  ~~~
  先校验remainingQuantity>0、数量范围和 `amount <= remainingAmount`；非最后一批不能静默截断金额。冻结适用的订单、运费、明细及支付余额与退款明细同事务。
- [ ] 退款申请幂等比较类型、原因、归一化商品数量和原支付单；USER不收任意金额。删除旧渠道申请/路径参数映射，保留回调。ADMIN差价和原运费专用入口也需受限额度及稳定requestId。
- [ ] 全部用例通过后审查每种退款 quota_kind 与金额/数量冻结是否完整；不通过改测试继续接受旧金额body。

### Task 8：履约记录与分批验收回库

**Files**
- Create: payment-demo/src/main/java/cc/ivera/service/fulfillment/FulfillmentService.java、FulfillmentServiceImpl.java、ReturnReceiptService.java、ReturnReceiptServiceImpl.java
- Create: payment-demo/src/main/java/cc/ivera/mapper/FulfillmentOperationMapper.java、ReturnReceiptMapper.java
- Create: payment-demo/src/main/java/cc/ivera/dto/ShipmentRequest.java、ReturnReceiptRequest.java
- Create: payment-demo/src/main/java/cc/ivera/controller/FulfillmentController.java、ReturnReceiptController.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/InventoryServiceImpl.java
- Create: payment-demo/src/test/java/cc/ivera/service/fulfillment/FulfillmentRaceTest.java、ReturnReceiptServiceTest.java

- [ ] 红测：发货与取消同数量只能一方成功；未到账退款可依取消证据回库；已发商品分批可售/不可售；同批同参重放、异参409、第二合法批次成功；越权/超收/超回库失败。
- [ ] 运行 mvn '-Dtest=FulfillmentRaceTest,ReturnReceiptServiceTest' test。
- [ ] 实现有稳定requestId的SHIP/RECEIVE/CANCEL记录及数量条件更新：
  ~~~sql
  UPDATE t_order_item
  SET cancel_confirmed_qty = cancel_confirmed_qty + #{qty}
  WHERE id = #{itemId}
    AND shipped_qty + cancel_confirmed_qty + #{qty} <= quantity;
  ~~~
  取消/发货竞争订单锁；只允许ACTIVE有效支付订单的合法剩余数量发货。
- [ ] Receipt、收货/验收/授权/回库量、库存及流水同事务，唯一键(refundItemId,receiptRequestId)。回库条件为本退款明细授权量，不得用refunded_qty。不同批次流水键不同，普通退款成功不直接加库存。
- [ ] 验证验收事件仅在退款执行条件满足时写提交Outbox；金额尚未退款时回库已完成不再次回库。记录不允许撤销已发生的不可逆履约/验收来释放资金义务。

### Task 9：统一退款提交与成功结算

**Files**
- Create: payment-demo/src/main/java/cc/ivera/service/refund/VerifiedRefundEvent.java、RefundCompletionService.java、RefundCompletionServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/service/refund/OrderRefundStatusService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/RefundApplicationServiceImpl.java、RefundInfoServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/wxpay/WxPayRefundService.java
- Modify: payment-demo/src/main/java/cc/ivera/service/impl/AliPayServiceImpl.java
- Modify: payment-demo/src/main/java/cc/ivera/mq/RefundSubmitConsumer.java、RefundStatusSyncConsumer.java
- Create: payment-demo/src/test/java/cc/ivera/service/refund/RefundCompletionServiceTest.java、CompensationReleaseTest.java

- [ ] 红测：审批无加库/HTTP；缺履约证据不调渠道；成功一次转完整额度；失败/UNKNOWN保留额度；零金额不调渠道；系统补偿不改正常退款汇总；ANOMALY_PENDING不借新锁库释放。
- [ ] 运行 mvn '-Dtest=RefundCompletionServiceTest,CompensationReleaseTest,RefundSubmitConsumerTest,RefundStatusSyncMessagingTest' test。
- [ ] 提交消费者按类型推进取消或等待验收，远程退款使用原PaymentOrder的商户配置/渠道单号和固定refundNo。未满足实物条件不是渠道失败，不生成无休止远程重试。
- [ ] 退款完成服务在订单统一锁内一次性 CAS 成功、冻结→已退、写必要Outbox/Inbox。普通退款成功仅记资金/数量；STOCK_SHORTAGE单独安全释放Reservation，DUPLICATE/LATE只结算原异常支付的资金。
- [ ] 接入微信通知、微信主动查询、支付宝同步退款响应/查单及Rabbit同步入口；任何旧直接updateRefundToSuccess都须委托同一完成服务。成功不能回退，临时失败不能解冻。

### Task 10：React 与 Vue API、用户/管理员页面对齐

**Files**
- Modify both API directories: payment-demo-react/src/api/ 与 payment-demo-vue/src/api/ 下 orderInfo.js、wxPay.js、aliPay.js、refundInfo.js、product.js、cart.js
- Create both API directories: paymentOrder.js、fulfillment.js
- Modify React pages: payment-demo-react/src/pages/Home.jsx、Cart.jsx、Orders.jsx、Refunds.jsx
- Create: payment-demo-react/src/pages/Products.jsx
- Modify Vue views: payment-demo-vue/src/views/index.vue、Cart.vue、Orders.vue、Refunds.vue
- Create: payment-demo-vue/src/views/Products.vue
- Modify: payment-demo-react/src/App.jsx、components/AppHeader.jsx、components/AppFooter.jsx、components/WxPayDialog.jsx
- Modify: payment-demo-vue/src/router/index.js、components/AppHeader.vue、components/AppFooter.vue
- Create: docs/testing/v060-browser-acceptance.md

- [ ] 将统一设计第9节各API写入两端对照清单，列USER/ADMIN权限、请求字段、幂等重试、金额/数量、异常状态和预计可见按钮。
- [ ] USER流程：后端签发订单键→下单→稳定支付请求号；订单内切换支付尝试；售罄禁加购；数量退款选择器；分别显示资金/实物进度。使用本地HTTP模拟重放验证键不会被刷新丢弃。
- [ ] ADMIN流程：商品/默认SKU余额和流水、退款类型/明细审批、发货、分批验收、差价/原运费操作、支付与退款查询；系统退款不提供再次审批。全部调用同一后端契约，保留现有UI库。
- [ ] 移除完整友情链接及版权行，管理员无购买入口；状态必须显示真实有效付款/异常付款，不能用单个旧orderStatus推断。旧金额申请与随机订单key代码静态扫描清零。
- [ ] 在React目录 npm run build；在Vue目录 npm run lint 与 npm run build。按清单通过两种角色、两端浏览器验证。构建通过不替代页面交互验收。

### Task 11：存量核对、迁移演练和对账验证

**Files**
- Modify: payment-demo/sql/migrations/20260904_v060_expand.sql、20260904_v060_verify.sql
- Create: payment-demo/sql/migrations/20260904_v060_backfill.sql
- Create: docs/deployment/v060-migration.md
- Create: payment-demo/src/test/java/cc/ivera/integration/TradeMigrationIT.java、PaymentReconciliationIT.java

- [ ] 准备隔离历史样本：正常付款、同渠道多付款、UNKNOWN、旧金额部分退款、缺库存流水和缺快照；给出预期映射或LEGACY_REVIEW差异，不根据当前价格推导。
- [ ] 测试增量建表/回填幂等、原渠道订单号保持、Reservation无二次锁/放库存、余额可核对；用mvn '-Dtest=TradeMigrationIT,PaymentReconciliationIT' test在显式隔离数据库运行。
- [ ] 回填只处理证据完备映射；未知历史退款不生成虚假明细。核对结束前保留旧字段，切换时仅新库存可写；历史净售出通过可验证期初快照及真实流水衔接。
- [ ] 文档明确备份/暂停业务写/可靠保存回调/逐阶段校验/恢复重放；新付款发生后前向恢复，不声称可直接回滚旧应用。线上迁移不在当前授权内自动执行。
- [ ] 演练正常/重复/迟到/补偿退款逐笔对账，验收每一真实渠道资金流水都可定位PaymentOrder而不是仅业务订单。

### Task 12：真实并发、全链路与发布材料

**Files**
- Create: payment-demo/src/test/java/cc/ivera/integration/InventoryOversellIT.java、OrderCreationIdempotencyIT.java、PaymentCloseConcurrencyIT.java、RefundQuotaConcurrencyIT.java、StockShortageCompensationIT.java、MessageRecoveryIT.java
- Modify: spec/README.md、spec/planned/PRODUCT_INVENTORY_MANAGEMENT_COMPAT_SPEC.md
- Update only relevant sections: payment-demo/README.md、payment-demo-react/README.md、payment-demo-vue/README.md
- Create: docs/testing/v060-verification-results.md

- [ ] 隔离MySQL验证50个请求抢10件恰好10件预占、20个同键并发只一单、支付/关单竞态、20次并发退款不超量、任何子项库存不足全额补偿；失败场景证明实际事务回滚。
- [ ] 隔离Rabbit验证无确认不SENT、发送后崩溃、Inbox过期租约、旧token完成失败、远程成功本地失败重试、分批回库重复消息。
- [ ] 运行 mvn test 和 mvn package；运行统一设计要求的IT与两端构建，保存命令、环境、通过数/失败原因；未验证的真实渠道明确列为未验证，不以Mock代替声明。
- [ ] 完成两端浏览器验收和独立代码审查，核对迁移材料、金额约束及新旧API；只能在实现/测试/部署验证一致后标记implemented。
- [ ] 交付可检查的代码、设计、迁移说明和验证结果；提交、合并、实际部署按用户授权执行，不自动操作数据库或付款渠道。

## 当前首个检查点

立即执行 Task 1。现有构造器缺参、V2/V3早成功和主动退款查询Inbox缺口必须先收尾；Task 1通过后才执行Task 2。本文复核不等于任何新任务已通过，不从旧任务完成数量推导新模型完成率。

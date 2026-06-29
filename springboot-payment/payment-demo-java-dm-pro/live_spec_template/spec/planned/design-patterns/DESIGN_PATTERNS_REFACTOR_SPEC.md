# 设计模式重构 SPEC

> 适用项目：Payment Demo V5  
> 状态：implemented  
> 领域（feature-domain）：design-patterns  
> 创建于：2026-06-15  
> 更新于：2026-06-15  
> 影响范围：backend | tests | spec

## 1. 背景 / 目标

当前项目存在以下结构性问题，通过引入经典设计模式进行行为保持的重构：

| 问题 | 现状 | 目标模式 |
|------|------|----------|
| 支付提供商接口不统一 | `AliPayService` 与 `WxPayOrderFacade`/`WxPayRefundFacade` 方法签名相似但无公共接口 | **Strategy 模式** |
| 状态管理基于字符串 | `OrderStatus`/`RefundStatus` 是字符串枚举，状态转换散落各处 | **State 模式** |
| 支付流程重复 | `AliPayServiceImpl` 和 `WxPayOrderService` 有相似的 lock→create→call→save 流程 | **Template Method 模式** |
| 通知处理耦合 | `WxPayNotifyHandler` 混合验签、幂等、处理逻辑 | **Chain of Responsibility 模式** |
| 退款流程过程式 | `RefundApplicationServiceImpl.approve()` 是长过程式方法 | **Command 模式** |

## 2. 设计模式方案

### 2.1 Strategy 模式 — 统一支付提供商接口

```
PaymentProvider (interface)
├── AliPayProviderAdapter  (适配 AliPayService)
└── WxPayProviderAdapter   (适配 WxPayOrderFacade + WxPayRefundFacade)
```

**统一方法签名：**
- `createPayment(Long productId, Long paymentAppId) → Object`
- `processPaymentNotification(Map<String, ?> params) → void`
- `executeRefund(RefundInfo) → void`
- `queryRefundStatusForSync(String refundNo) → RefundStatusSyncResult`
- `cancelOrder(String orderNo) → void`
- `queryOrder(String orderNo) → String`
- `checkOrderStatus(String orderNo) → void`
- `queryRefund(String refundNo) → String`

**Provider 选择策略：** `PaymentProviderFactory` 根据 `PayType` 自动注入并返回对应 Provider。

### 2.2 State 模式 — 订单/退款状态机

```
OrderStateMachine (静态工厂 + 内部状态类)
├── NotPaidState       (未支付)
├── PaidSuccessState   (支付成功)
├── ClosedState        (超时已关闭)
├── CanceledState      (用户已取消)
├── RefundProcessingState (退款中)
├── PartialRefundState (部分退款)
├── FullRefundState    (已退款)
└── RefundAbnormalState (退款异常)

RefundStateMachine (静态工厂 + 内部状态类)
├── CreatedRefundState    (CREATED)
├── ProcessingRefundState (PROCESSING)
├── SuccessRefundState    (SUCCESS)
├── FailedRefundState     (FAILED)
├── ClosedRefundState     (CLOSED)
└── AbnormalRefundState   (ABNORMAL)
```

### 2.3 Template Method 模式 — 支付流程模板

```
AbstractPaymentFlow<R>
├── execute(): 模板方法骨架
│   ├── preValidate()          // 前置校验
│   ├── resolvePaymentConfig() // 解析支付配置
│   ├── createOrReuseOrder()   // 创建/复用订单
│   ├── checkCachedResult()    // 检查缓存结果
│   ├── callPaymentApi()       // 调用支付 API (子类实现)
│   ├── savePaymentEntry()     // 保存支付入口
│   └── postProcess()          // 后置处理
```

### 2.4 Chain of Responsibility 模式 — 通知处理链

```
NotificationHandler (interface)
├── IdempotencyCheckHandler     // 幂等检查 (Redis SETNX)
├── BusinessValidationHandler   // 业务校验 (订单存在性)
└── (可扩展: SignatureVerificationHandler, OrderStatusUpdateHandler, PaymentRecordHandler)

NotificationChain — 按序执行 Handler，任一失败则中断
NotificationContext — 在链节点间传递数据
```

### 2.5 Command 模式 — 支付操作命令化

```
PaymentCommand<T> (interface)
├── getLockKey() / getLockWaitMs() / getLockLeaseMs()  // 锁配置
├── requiresTransaction()  // 事务配置
└── execute()  // 命令执行

PaymentCommandInvoker — 统一封装 分布式锁 + 事务模板
├── RefundExecutionCommand — 退款执行命令
└── (可扩展: CreateOrderCommand, CancelOrderCommand, QueryOrderCommand)
```

## 3. 兼容性声明

| 维度 | 策略 |
|------|------|
| REST API | **不变** — 所有 Controller 路由、请求/响应结构保持兼容 |
| 数据库 Schema | **不变** — 实体字段、表结构不变 |
| 状态值 | **不变** — `OrderStatus`/`RefundStatus` 字符串值不变 |
| 配置 | **不变** — `application.yml`、`PaymentConfigLoader` 行为不变 |
| 分布式锁 Key | **不变** — 锁 key 格式不变 |
| RabbitMQ | **不变** — Exchange/Queue/RoutingKey 不变 |
| 异常语义 | **不变** — `BizException` 抛出场景不变 |

## 4. 实施计划（已完成）

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | SDD — 本 Spec 文档 | done |
| Phase 2 | TDD — 补充特征测试 | done |
| Phase 3 | Strategy 模式实现 | done |
| Phase 4 | State 模式实现 | done |
| Phase 5 | Template Method 模式实现 | done |
| Phase 6 | Chain of Responsibility 模式实现 | done |
| Phase 7 | Command 模式实现 | done |
| Phase 8 | 全量测试验证 + Spec 对账 | done |

## 5. 实现锚点

| 模式 | 新增文件 |
|------|----------|
| Strategy | [PaymentProvider.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/provider/PaymentProvider.java), [AliPayProviderAdapter.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/provider/AliPayProviderAdapter.java), [WxPayProviderAdapter.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/provider/WxPayProviderAdapter.java), [PaymentProviderFactory.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/provider/PaymentProviderFactory.java) |
| State | [OrderState.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/state/OrderState.java), [OrderStateMachine.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/state/OrderStateMachine.java), [RefundState.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/state/RefundState.java), [RefundStateMachine.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/state/RefundStateMachine.java) |
| Template Method | [AbstractPaymentFlow.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/flow/AbstractPaymentFlow.java) |
| Chain of Resp. | [NotificationHandler.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/notification/NotificationHandler.java), [NotificationChain.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/notification/NotificationChain.java), [NotificationContext.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/notification/NotificationContext.java), [IdempotencyCheckHandler.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/notification/IdempotencyCheckHandler.java), [BusinessValidationHandler.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/notification/BusinessValidationHandler.java) |
| Command | [PaymentCommand.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/command/PaymentCommand.java), [PaymentCommandInvoker.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/command/PaymentCommandInvoker.java), [RefundExecutionCommand.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/main/java/cc/ivera/service/command/RefundExecutionCommand.java) |
| 特征测试 | [DesignPatternRefactorCharacterizationTest.java](file:///d:/code/demo/springboot-payment/payment-demo-java-dm-v1/payment-demo/src/test/java/cc/ivera/characterization/DesignPatternRefactorCharacterizationTest.java) |

## 6. 验收标准

- [x] 所有现有特征测试通过（行为不变）
- [x] 新增设计模式相关单元测试通过
- [x] `PublicApiCharacterizationTest` 全绿 (9 tests)
- [x] `InfrastructureBehaviorCharacterizationTest` 全绿 (10 tests)
- [x] `PaymentFlowCharacterizationTest` 全绿 (46 tests)
- [x] `DesignPatternRefactorCharacterizationTest` 全绿 (37 tests)
- [x] 编译无错误
- [x] Spec 对账完成（本 spec 从 planned 移至 implemented）
- [x] **总计: 102 tests, 0 failures, 0 errors**

# Payment Demo V5 当前行为 SPEC

> 适用项目：Payment Demo V5  
> 状态：implemented  
> 领域（feature-domain）：current-behavior  
> 更新于：2026-06-09  
> 负责人：<待填写>  
> 影响范围：backend | react-frontend | vue-frontend | database | redis | rabbitmq | external-provider | docs | tests

## 1. 背景 / 目标

本 spec 记录当前支付演示系统已经存在的行为面，作为后续重构前的特征测试依据。这里不判断行为是否合理，也不提出重构方案，只把当前对外接口、状态变化、事件、缓存、配置和可疑行为写清楚。

当前系统由 Spring Boot 后端、React/Vue 两套前端、MySQL、Redis、RabbitMQ、微信支付 V2/V3、支付宝组成。业务主线是：前端选择支付应用并发起下单，后端创建或复用订单，调用支付渠道生成支付入口；支付或退款回调通过验签、幂等、行锁、状态条件更新同步本地状态；延迟关单通过 RabbitMQ 触发主动查单。

## 2. 当前现状

### 2.1 入口清单

| 入口 | 类型 | 当前调用方 | 备注 |
|---|---|---|---|
| `GET /api/product/test` | REST | 人工/前端调试 | 返回 `R`，data 中含 `message=hello` 与当前时间 |
| `GET /api/product/list` | REST | React/Vue 首页 | 返回 key 为 `productList` 的商品列表 |
| `GET /api/order-info/list` | REST | React/Vue 订单页 | 返回 key 为 `list` 的订单列表 |
| `GET /api/order-info/query-order-status/{orderNo}` | REST | 前端轮询 | 成功返回 code `0`，未支付/其他/空状态均返回 code `101` 与“支付中......” |
| `POST /api/wx-pay/native/{productId}` | REST | 前端 | 微信 V3 Native 下单，支持 `paymentAppId` |
| `POST /api/wx-pay/native/notify` | Provider callback | 微信支付 | V3 支付通知，JSON 响应 |
| `POST /api/wx-pay-v2/native/{productId}` | REST | 前端 | 微信 V2 Native 下单，支持 `paymentAppId` |
| `POST /api/wx-pay-v2/native/notify` | Provider callback | 微信支付 | V2 支付通知，XML 响应 |
| `POST /api/ali-pay/trade/page/pay/{productId}` | REST | 前端 | 支付宝电脑网站支付，返回 HTML form 字符串 |
| `POST /api/ali-pay/trade/notify` | Provider callback | 支付宝 | 成功响应字符串 `success`，失败响应 `failure` |
| `POST /api/refund-info/apply` | REST | 前端 | 新退款申请入口，JSON body |
| `POST /api/wx-pay/refunds` | REST | 前端/旧调用方 | 兼容退款申请入口，JSON body |
| `POST /api/ali-pay/trade/refund` | REST | 前端/旧调用方 | 兼容退款申请入口，JSON body |
| `POST /api/refund-info/apply/{orderNo}/{reason}` | REST | 旧调用方 | 兼容旧入口，退款金额传 `null` |
| `POST /api/refund-info/approve/{refundNo}` | REST | 管理页 | 审核通过并提交渠道退款 |
| `POST /api/refund-info/reject/{refundNo}` | REST | 管理页 | 审核拒绝，退款单变关闭 |
| `POST /api/payment-config/reload` | REST | 配置页/人工 | 重新加载 DB 支付配置缓存 |
| `GET /api/payment-config/apps` | REST | 配置页/人工 | 现状：返回运行时 `PaymentAppConfig` 对象 |
| `payment.order.close.release.queue` | RabbitMQ | `OrderCloseConsumer` | 延迟关单死信消费入口 |

### 2.2 实现锚点

| 层 | 文件 / 类 / 函数 | 现有职责 |
|---|---|---|
| Controller | `ProductController` | 商品列表和测试接口 |
| Controller | `OrderInfoController` | 订单列表、本地订单状态轮询 |
| Controller | `WxPayController` / `WxPayV2Controller` | 微信下单、通知、查单、退款通知、账单 |
| Controller | `AliPayController` | 支付宝下单、通知、查单、关单、账单 |
| Controller | `RefundApplicationController` / `RefundInfoController` | 退款申请、审核、查询、对账 |
| Controller | `PaymentChannelController` / `PaymentAppController` / `PaymentConfigController` | 支付配置维护和缓存重载 |
| Service | `OrderInfoServiceImpl` | 创建/复用订单、保存二维码、状态更新、事务后发关单消息 |
| Service | `PaymentInfoServiceImpl` | 记录支付流水；重复流水按幂等成功吞掉 |
| Service | `RefundInfoServiceImpl` / `RefundApplicationServiceImpl` | 退款申请、审核、状态同步、渠道补录 |
| Service | `OrderRefundStatusService` | 汇总退款状态并刷新订单状态 |
| MQ | `OrderCloseMessageServiceImpl` / `OrderCloseConsumer` | 发送和消费延迟关单消息 |
| Cache | `PaymentConfigLoader` | 加载并缓存启用支付渠道和支付应用配置 |
| Lock | `RedissonDistributedLockTemplate` | 包装 Redisson 分布式锁 |

### 2.3 现有可疑行为

- 「现状」`query-order-status` 对 `NOTPAY`、空状态、关闭、取消等非成功状态都返回 code `101` 和“支付中......”。
- 「现状」`GET /api/payment-config/apps` 返回完整 `PaymentAppConfig`，包含商户配置字段；测试只锁住返回结构，不判断安全性。
- 「现状」微信 V2 通知如果 XML 解析失败，直接返回微信失败 XML，不触发 Redis、锁、数据库或业务服务。
- 「现状」支付流水插入遇到唯一约束冲突时吞掉 `DuplicateKeyException`，日志记录“支付流水已存在”，调用方视为幂等成功。
- 「现状」订单退款汇总的状态优先级是：全额成功 > 部分成功 > 处理中 > 异常 > 恢复支付成功。
- 「现状」旧退款申请路径 `{orderNo}/{reason}` 会以 `refundAmount=null` 调用服务，表示退剩余可退金额。

## 3. 契约

### 3.1 响应结构

普通 REST 接口返回 `R<T>`：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

当前 `R.ok()` 默认：

- `code = 0`
- `message = "成功"`
- `data = HashMap`

当前 `R.error()` 默认：

- `code = -1`
- `message = "失败"`
- `data = HashMap`

### 3.2 状态迁移

订单状态当前用中文文案作为持久化值：

| 场景 | 当前行为 |
|---|---|
| 本地轮询查到支付成功 | `code=0,message=支付成功` |
| 本地轮询查到未支付 | `code=101,message=支付中......` |
| 本地轮询查到空或其他非成功状态 | `code=101,message=支付中......` |
| 延迟关单消费非 `NOTPAY` 订单 | 直接忽略 |
| 延迟关单消费 `WXPAY` 订单 | 调用微信查单同步 |
| 延迟关单消费 `ALIPAY` 订单 | 调用支付宝查单同步 |

退款状态汇总当前规则：

| 本地退款汇总 | 订单目标状态 |
|---|---|
| 成功退款金额 >= 订单金额 | `REFUND_SUCCESS` |
| 成功退款金额 > 0 且未全额 | `PARTIAL_REFUND` |
| 无成功退款且处理中金额 > 0 | `REFUND_PROCESSING` |
| 无成功/处理中且异常金额 > 0 | `REFUND_ABNORMAL` |
| 以上都没有 | `SUCCESS` |

### 3.3 事件和日志

- 新订单在事务提交后发送 `OrderCloseMessage` 到 `payment.order.close.event.exchange`，routing key 为 `payment.order.close.delay`。
- `OrderCloseMessage` 当前包含 `orderNo` 和 `paymentType`。
- 支付流水重复插入时记录 info 日志，包含 `orderNo`、`paymentType`、`transactionId`。

## 4. 隐式输入输出 / 状态地图

| 类型 | 名称 / 位置 | 读 | 写 | 生命周期 | 备注 |
|---|---|---|---|---|---|
| DB | `t_order_info` | 订单、支付、退款、延迟关单 | 订单创建、状态更新、二维码保存 | 持久 | `order_status` 为中文文案 |
| DB | `t_payment_info` | 支付流水查询 | 支付通知、查单同步 | 持久 | 唯一约束兜底重复通知 |
| DB | `t_refund_info` | 退款列表、审核、同步 | 退款申请、审核、渠道同步 | 持久 | 审核状态和退款状态分离 |
| DB | `t_payment_channel/t_payment_app` | `PaymentConfigLoader` | 配置 CRUD | 持久 + 运行时缓存 | JSON 字段决定支付配置 |
| Redis | `payment:wx:notify:processed:*` | 微信 V3 通知 | 微信 V3 通知 | 24 小时 | idempotency |
| Redis | `payment:wx:v2:notify:processed:*` | 微信 V2 通知 | 微信 V2 通知 | 24 小时 | idempotency |
| Redis | Redisson lock key | 关键业务路径 | 关键业务路径 | 锁生命周期 | 订单、通知、退款、查单 |
| RabbitMQ | `payment.order.close.*` | 延迟关单消费者 | 订单创建后 producer | TTL 后死信 | 延迟关单 |
| Config | `application.yml` | Spring/Redis/RabbitMQ/DB | 人工 | 启动期 | 固定环境地址 |
| Config | `wxpay.properties` / `alipay-sandbox.properties` | 支付 fallback | 人工 | 启动期 | 当前仍与 DB 配置并存 |
| Log | Slf4j | 运维/调试 | 多数 Controller/Service | 日志策略 | 可能包含渠道返回体 |

## 5. 多套实现 / 多套规则并存

- 微信 V2 和 V3 并存，各自通知入口、验签、报文格式不同。
- 支付配置存在 DB 动态配置和 properties fallback 两套来源。
- React 和 Vue 两套前端并存，均调用 `http://localhost:8080`。
- 退款申请同时存在 JSON body 新入口和 path variable 旧入口。
- 幂等同时依赖 Redis 通知幂等、Redisson 锁、数据库行锁、状态条件更新、唯一约束。
- 支付宝存在 Spring Bean `AlipayClient` 和业务内按应用配置动态创建客户端两种形态。

## 6. 当前特征测试锚点

| 测试文件 | 锁住行为 |
|---|---|
| `PublicApiCharacterizationTest` | REST/controller 返回结构、兼容退款入口、配置缓存返回结构 |
| `InfrastructureBehaviorCharacterizationTest` | 微信 V2 拒绝路径、支付流水重复日志与吞异常、RabbitMQ 事件、退款汇总状态优先级 |

## 7. 验收标准

- [x] 不连接真实 MySQL、Redis、RabbitMQ、微信或支付宝。
- [x] 每条测试使用 mock 或 standalone 对象隔离状态。
- [x] 可疑行为在测试名或注释中标明“现状”。
- [x] 覆盖 public API 返回结构、拒绝路径、事件和日志行为面。
- [x] 不修改业务代码。

## 8. 变更记录

| 日期 | 状态 | 改了什么 | 关联 issue / PR |
|---|---|---|---|
| 2026-06-09 | implemented | 初稿，记录当前行为并关联第一批特征测试 | 当前任务 |


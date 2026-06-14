# Payment Demo Go 当前行为 SPEC

> 适用项目：`payment-demo-go` + React/Vue 前端  
> 状态：implemented  
> 领域：current-behavior  
> 更新于：2026-06-12  
> 影响范围：backend | react-frontend | vue-frontend | database | redis | rabbitmq | external-provider | tests

## 1. 背景 / 目标

本 spec 记录当前工作区内 Go 版支付演示系统的既有行为，用于后续重构前的特征测试。这里不判断现状是否合理，也不提出修复方案；所有看起来可疑的行为，在重构前都应先用测试原样锁住。

当前可运行后端是 `payment-demo-go/`，前端同时存在 `payment-demo-react/` 和 `payment-demo-vue/`。目录名来自 Spring Boot 版本，但本 spec 以当前 Go 代码为准。

## 2. 入口清单

| 入口 | 类型 | 当前调用方 | 备注 |
|---|---|---|---|
| `cmd/server/main.go` | 进程入口 | 运维/本地启动 | 初始化配置、MySQL、Redis、RabbitMQ、微信/支付宝客户端、Gin 路由 |
| `GET /api/product/test` | REST | 人工/调试 | 返回统一响应，data 中包含 `message=hello` 和当前时间 |
| `GET /api/product/list` | REST | React/Vue 首页 | 返回 `data.productList` |
| `GET /api/order-info/list` | REST | React/Vue 订单页 | 返回 `data.list` |
| `GET /api/order-info/query-order-status/:orderNo` | REST | React/Vue 支付轮询 | 只有本地状态为 `支付成功` 时返回 `code=0`，其他状态返回 `code=101` |
| `POST /api/wx-pay/native/:productId` | REST | 前端 | 微信 V3 Native 下单 |
| `POST /api/wx-pay/native/notify` | Provider callback | 微信支付 | 微信 V3 支付通知，JSON 响应 |
| `POST /api/wx-pay-v2/native/:productId` | REST | 前端 | 微信 V2 Native 下单 |
| `POST /api/wx-pay-v2/native/notify` | Provider callback | 微信支付 | 微信 V2 支付通知，XML 响应 |
| `POST /api/wx-pay/cancel/:orderNo` | REST | 前端订单页 | 微信关单，并把本地未支付订单标记为用户取消 |
| `GET /api/wx-pay/query/:orderNo` | REST | 人工/调试 | 微信查单，返回 `data.result` |
| `GET /api/wx-pay/check-order-status/:orderNo` | REST | 人工/调试 | 微信查单并同步本地状态 |
| `GET /api/wx-pay/query-refund/:refundNo` | REST | 人工/调试 | 微信查询退款 |
| `POST /api/wx-pay/refunds/notify` | Provider callback | 微信支付 | 微信退款通知，复用 V3 通知验签/解密流程 |
| `GET /api/wx-pay/querybill/:billDate/:type` | REST | 人工/调试 | 获取微信账单下载 URL |
| `GET /api/wx-pay/downloadbill/:billDate/:type` | REST | React/Vue 账单页 | 下载微信账单内容 |
| `POST /api/wx-pay/jsapi` | REST | 可能的旧/移动端调用方 | 生成微信 JSAPI 支付参数 |
| `POST /api/wx-pay/jsapi/notify/v1` | Provider callback | 可能的旧调用方 | 复用微信 V3 支付通知处理 |
| `POST /api/ali-pay/trade/page/pay/:productId` | REST | 前端首页 | 支付宝电脑网站支付，返回 `data.formStr` |
| `POST /api/ali-pay/trade/notify` | Provider callback | 支付宝 | 成功返回字符串 `success`，失败返回 `failure` |
| `POST /api/ali-pay/trade/close/:orderNo` | REST | 前端订单页 | 支付宝关单，并把本地未支付订单标记为用户取消 |
| `GET /api/ali-pay/trade/query/:orderNo` | REST | 人工/调试 | 支付宝查单 |
| `GET /api/ali-pay/trade/fastpay/refund/:refundNo` | REST | 人工/调试 | 支付宝查询退款 |
| `GET /api/ali-pay/bill/downloadurl/query/:billDate/:type` | REST | React/Vue 账单页 | 获取支付宝账单下载 URL |
| `POST /api/refund-info/apply` | REST | 新调用方 | JSON body 退款申请入口 |
| `POST /api/wx-pay/refunds` | REST | React/Vue 订单页 | 兼容退款申请入口 |
| `POST /api/ali-pay/trade/refund` | REST | React/Vue 订单页 | 兼容退款申请入口 |
| `POST /api/refund-info/apply/:orderNo/:reason` | REST | 旧调用方 | 旧退款申请入口，退款金额传 `nil` |
| `POST /api/wx-pay/refunds/:orderNo/:reason` | REST | 旧调用方 | 旧微信退款申请入口 |
| `POST /api/ali-pay/trade/refund/:orderNo/:reason` | REST | 旧调用方 | 旧支付宝退款申请入口 |
| `GET /api/refund-info/list` | REST | 前端/管理页 | 全部退款列表 |
| `GET /api/refund-info/list/:orderNo` | REST | 前端订单页 | 指定订单退款列表 |
| `POST /api/refund-info/approve/:refundNo` | REST | 管理页 | 审核通过并提交渠道退款 |
| `POST /api/refund-info/reject/:refundNo` | REST | 管理页 | 审核拒绝 |
| `POST /api/refund-info/query/:refundNo` | REST | 管理页/人工 | 查询并同步单笔退款状态 |
| `POST /api/refund-info/reconcile/:orderNo` | REST | 管理页/人工 | 按订单对账退款状态 |
| `payment.order.close.release.queue` | RabbitMQ | 延迟关单消费者 | TTL 死信后的关单入口 |

## 3. 职责板块

| 职责 | 实现锚点 | 当前行为 |
|---|---|---|
| 启动组装 | `cmd/server/main.go` | 启动时必须连接 MySQL、Redis、RabbitMQ，并初始化微信 V3/V2、支付宝客户端 |
| 配置加载 | `internal/config/config.go` | 默认读取 `config/application.yml`、`config/wxpay.properties`、`config/alipay-sandbox.properties`，可用环境变量覆盖 |
| HTTP 路由 | `internal/handler/handler.go` | 注册所有 REST 和渠道回调入口，普通异常由 `recoverJSON` 转成 HTTP 200 + `code=-1` |
| 统一响应 | `internal/response/response.go` | `OK()` 为 `code=0,message=成功,data={}`；`Error(msg)` 为 `code=-1,message=msg,data={}` |
| 订单 | `internal/service/order.go` | 按 `product_id + payment_type + 未支付` 复用最近一笔订单；新订单发送延迟关单消息 |
| 微信支付 | `internal/service/wx_pay.go`、`internal/pay/wxv2.go`、`internal/pay/wxv3.go` | 微信 V3/V2 并存；通知走 Redis 幂等、验签、DB 行锁、状态条件更新 |
| 支付宝 | `internal/service/ali_pay.go`、`internal/pay/alipay.go` | 支付宝电脑网站支付、通知、查单、关单、退款、账单 |
| 支付流水 | `internal/service/payment.go` | 写入 `t_payment_info`，唯一约束冲突按幂等成功吞掉 |
| 退款申请/审核 | `internal/service/refund_application.go`、`internal/service/refund_info.go` | 退款申请先入本地审核流，审核通过后提交渠道退款 |
| 退款汇总 | `internal/service/refund_status.go` | 按成功/处理中/异常金额汇总刷新订单状态 |
| 延迟关单 | `internal/mq/order_close.go`、`internal/service/order_close.go` | RabbitMQ TTL + 死信队列，到期后按支付方式查单/关单 |
| 前端 | `payment-demo-react/src/api/*.js`、`payment-demo-vue/src/api/*.js` | 两套前端都固定请求 `http://localhost:8080` |

## 4. 契约

### 4.1 普通 REST 响应

成功：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

失败：

```json
{
  "code": -1,
  "message": "失败原因",
  "data": {}
}
```

现状：普通业务失败通常仍返回 HTTP 200，由 `code` 判断业务成功与否。

### 4.2 渠道回调响应

| 回调 | 成功响应 | 失败/拒绝响应 |
|---|---|---|
| 微信 V3 支付/退款通知 | HTTP 200 JSON：`{"code":"SUCCESS","message":"成功"}` | HTTP 500 JSON：`{"code":"ERROR","message":"失败"}` |
| 微信 V2 支付通知 | HTTP 200 XML：`SUCCESS/OK` | HTTP 200 XML：`FAIL/失败`、`FAIL/验签失败`、`FAIL/金额校验失败` |
| 支付宝支付通知 | HTTP 200 字符串 `success` | HTTP 200 字符串 `failure` |

### 4.3 状态值

订单状态使用中文字符串持久化：

| 常量 | 当前值 |
|---|---|
| `OrderStatusNotPay` | `未支付` |
| `OrderStatusSuccess` | `支付成功` |
| `OrderStatusClosed` | `超时已关闭` |
| `OrderStatusCancel` | `用户已取消` |
| `OrderStatusRefundProcessing` | `退款中` |
| `OrderStatusPartialRefund` | `部分退款` |
| `OrderStatusRefundSuccess` | `已退款` |
| `OrderStatusRefundAbnormal` | `退款异常` |

退款审核状态和退款状态使用英文枚举。

## 5. 状态地图

| 类型 | 名称 / 位置 | 读 | 写 | 生命周期 | 备注 |
|---|---|---|---|---|---|
| DB | `t_product` | 商品列表、下单 | 初始化脚本 | 持久 | 初始化 4 条课程，价格为分 |
| DB | `t_order_info` | 列表、轮询、查单、退款、关单 | 下单、通知、查单、取消、退款汇总 | 持久 | `uk_order_no` 防重复订单号 |
| DB | `t_payment_info` | 排障/审计 | 支付通知、主动查单同步 | 持久 | `uk_order_payment_type`、`uk_transaction_id_payment_type` 兜底幂等 |
| DB | `t_refund_info` | 退款列表、审核、对账 | 退款申请、审核、渠道同步 | 持久 | `uk_refund_no`、`uk_refund_id` 兜底幂等 |
| Redis | `payment:wx:notify:processed:*` | 微信 V3 通知 | 微信 V3 通知 | 24 小时 | 值先为 `processing`，成功后为 `processed` |
| Redis | `payment:wx:v2:notify:processed:*` | 微信 V2 通知 | 微信 V2 通知 | 24 小时 | 使用 `transaction_id` 作为 id |
| Redis | `payment:*` 锁 key | 关键业务路径 | 关键业务路径 | lease 到期或 Lua 释放 | `SETNX` + UUID value |
| RabbitMQ | `payment.order.close.event.exchange` | RabbitMQ | 新订单创建 | 持久消息 | 生产延迟关单消息 |
| RabbitMQ | `payment.order.close.delay.queue` | RabbitMQ | 新订单创建 | TTL 队列 | `x-message-ttl` 来自配置 |
| RabbitMQ | `payment.order.close.release.queue` | 消费者 | 死信交换机 | 长期 | 到期关单消费入口 |
| Config | `config/application.yml` | 启动期 | 人工 | 启动期 | Spring 风格配置，Go 代码只读部分字段 |
| Config | `wxpay.properties` / `alipay-sandbox.properties` | 启动期、渠道调用 | 人工 | 启动期 | 包含商户号、密钥、回调域名 |
| Log | `log.Printf`、GORM logger | 运维/调试 | 启动、MQ、SQL | 运行期 | 当前没有统一审计表 |

## 6. 多套实现 / 多套规则并存

- 微信 V3 与微信 V2 并存，签名、报文、通知响应格式不同。
- 微信 Native、微信 JSAPI、支付宝电脑网站支付并存。
- React 与 Vue 两套前端并存，接口封装基本同构。
- 退款入口存在 JSON body 新入口、支付渠道兼容入口、path variable 旧入口。
- 幂等规则同时依赖 Redis 通知 key、Redis 分布式锁、数据库行锁、状态条件更新、唯一索引。
- 配置文件是 Spring Boot 风格，实际运行时代码是 Go。
- 普通 REST 响应、微信 JSON/XML 回调响应、支付宝字符串回调响应三套返回规则并存。

## 7. 可疑行为清单

以下行为看起来可能需要后续评估，但当前测试必须先原样锁住：

- 现状：普通失败响应多数是 HTTP 200 + `code=-1`。
- 现状：本地订单轮询只要不是 `支付成功`，统一返回 `code=101,message=支付中......`。
- 现状：下单按 `product_id + payment_type + 未支付` 复用最近订单，没有用户维度。
- 现状：微信 V3/V2 都使用支付类型 `微信`，且复用同一个 `code_url` 字段。
- 现状：`SaveCodeURL` 只在 `code_url` 为空时写入，已有值不会刷新。
- 现状：支付流水重复插入通过错误字符串包含 `Duplicate` 判断并吞掉。
- 现状：支付宝查单非成功响应返回空字符串和 nil error。
- 现状：退款申请会把待审核退款计入占用可退金额。
- 现状：支付宝退款状态只有 `REFUND_SUCCESS` 映射成本地成功，其他状态映射为空。
- 现状：微信退款状态 `CHANGE` 映射为本地 `ABNORMAL`，`REFUNDCLOSE` 映射为本地 `CLOSED`。

## 8. 当前特征测试锚点

| 测试文件 | 锁住行为 |
|---|---|
| `internal/response/response_characterization_test.go` | 普通 REST 统一响应结构 |
| `internal/handler/handler_characterization_test.go` | public API 测试接口、HTTP 200 错误结构、CORS、微信 V2/V3 拒绝路径 |
| `internal/config/config_characterization_test.go` | 配置环境变量覆盖、默认端口、默认关单延迟、私钥相对路径解析 |
| `internal/lock/redis_lock_characterization_test.go` | Redis 锁空 key 拒绝路径，不触碰真实 Redis |
| `internal/mq/order_close_characterization_test.go` | 延迟关单事件名称和消息 JSON 格式 |
| `internal/pay/wxv2_characterization_test.go` | 微信 V2 签名/XML helper 的当前格式 |
| `internal/service/refund_characterization_test.go` | 退款状态映射、可同步状态集合、支付流水 duplicate 判断 |

## 9. 验收标准

- [x] 不连接真实 MySQL、Redis、RabbitMQ、微信或支付宝。
- [x] 测试需要读写环境变量时使用 `t.Setenv`，文件配置使用 `t.TempDir`。
- [x] 可疑行为在测试名或注释中标明 `CurrentBehavior` 或「现状」。
- [x] 覆盖 public API、返回结构、拒绝路径、事件消息格式和配置隐式输入。
- [x] 不修改业务代码。

## 10. 变更记录

| 日期 | 状态 | 改了什么 |
|---|---|---|
| 2026-06-12 | implemented | 新增 Go 当前行为 spec 和第一批特征测试锚点 |

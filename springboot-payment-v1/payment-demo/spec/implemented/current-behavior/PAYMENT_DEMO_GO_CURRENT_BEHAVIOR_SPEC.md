# Payment Demo Go 当前行为 SPEC

## 0. 元信息

- **状态**：implemented
- **领域（feature-domain）**：current-behavior
- **更新于**：2026-06-12
- **负责人**：待定
- **关联 issue / PR / 任务**：建立最小 spec 状态账本
- **影响范围**：backend | react-frontend | vue-frontend | database | redis | rabbitmq | external-provider | docs | tests

## 1. 背景 / 目标

当前仓库包含 Go 后端 `payment-demo-go/`、React 前端 `payment-demo-react/`、Vue 前端 `payment-demo-vue/`。目录名来自 Spring Boot 版本，但当前可运行后端以 Go 实现为准。

本 spec 锁住当前支付演示系统的既有行为，作为后续安全重构和缺陷修复的对账依据。这里不判断现状是否合理；可疑行为必须先通过特征测试原样保护，再讨论是否变更。

## 2. 当前现状

### 2.1 入口清单

| 入口 | 类型 | 当前调用方 | 备注 |
|---|---|---|---|
| `cmd/server/main.go` | 进程入口 | 本地/运维启动 | 初始化配置、MySQL、Redis、RabbitMQ、支付客户端和 Gin 路由 |
| `GET /api/product/test` | REST | 人工/调试 | 返回 `message=hello` 和当前时间 |
| `GET /api/product/list` | REST | React/Vue 首页 | 返回 `data.productList` |
| `GET /api/order-info/list` | REST | React/Vue 订单页 | 返回 `data.list` |
| `GET /api/order-info/query-order-status/:orderNo` | REST | 前端支付轮询 | 只有 `支付成功` 返回 `code=0`；其他状态返回 `code=101` |
| `POST /api/wx-pay/native/:productId?paymentAppId=...` | REST | 前端首页 | 微信 V3 Native 下单，`paymentAppId` 必填 |
| `POST /api/wx-pay-v2/native/:productId?paymentAppId=...` | REST | 前端首页 | 微信 V2 Native 下单，`paymentAppId` 必填 |
| `POST /api/wx-pay/native/notify` | Provider callback | 微信支付 | 微信 V3 支付通知 |
| `POST /api/wx-pay-v2/native/notify` | Provider callback | 微信支付 | 微信 V2 支付通知 |
| `POST /api/wx-pay/refunds/notify` | Provider callback | 微信支付 | 微信 V3 退款通知 |
| `POST /api/ali-pay/trade/page/pay/:productId?paymentAppId=...` | REST | 前端首页 | 支付宝电脑网站支付，`paymentAppId` 必填，返回 HTML form |
| `POST /api/ali-pay/trade/notify` | Provider callback | 支付宝 | 支付宝支付通知 |
| `POST /api/wx-pay/cancel/:orderNo` | REST | 前端订单页 | 微信取消未支付订单 |
| `POST /api/ali-pay/trade/close/:orderNo` | REST | 前端订单页 | 支付宝取消未支付订单 |
| `GET /api/wx-pay/downloadbill/:billDate/:type` | REST | React/Vue 账单页 | 下载微信账单内容 |
| `GET /api/ali-pay/bill/downloadurl/query/:billDate/:type` | REST | React/Vue 账单页 | 获取支付宝账单下载 URL |
| `POST /api/refund-info/apply` | REST | 新调用方 | JSON body 退款申请入口 |
| `POST /api/wx-pay/refunds` | REST | React/Vue 订单页 | 兼容退款申请入口 |
| `POST /api/ali-pay/trade/refund` | REST | React/Vue 订单页 | 兼容退款申请入口 |
| `POST /api/refund-info/apply/:orderNo/:reason` | REST | 旧调用方 | 旧退款申请入口，退款金额为剩余可退金额 |
| `GET /api/refund-info/list` | REST | 管理/前端 | 全部退款列表 |
| `GET /api/refund-info/list/:orderNo` | REST | 前端订单页 | 指定订单退款列表 |
| `POST /api/refund-info/approve/:refundNo` | REST | 管理页 | 审核通过并提交渠道退款 |
| `POST /api/refund-info/reject/:refundNo` | REST | 管理页 | 审核拒绝 |
| `POST /api/refund-info/query/:refundNo` | REST | 管理/人工 | 查询并同步单笔退款 |
| `POST /api/refund-info/reconcile/:orderNo` | REST | 管理/人工 | 按订单对账退款 |
| `payment.order.close.release.queue` | RabbitMQ | 延迟关单消费者 | TTL 死信后的关单入口 |
| `/`、`/orders`、`/download`、`/success` | UI route | React/Vue 用户 | 两套前端路由基本同构 |

### 2.2 实现锚点

| 层 | 文件 / 函数 | 现有职责 |
|---|---|---|
| Startup | `payment-demo-go/cmd/server/main.go` | 装配配置、DB、Redis、MQ、支付客户端、HTTP 路由 |
| Config | `internal/config/config.go` | 读取 YAML/properties，解析 DSN、Redis、RabbitMQ、支付配置 |
| HTTP | `internal/handler/handler.go` | 注册 public API、解析请求、渠道回调响应、Redis 通知幂等 |
| Response | `internal/response/response.go` | 普通 REST 统一响应结构 |
| Order | `internal/service/order.go` | 创建/复用订单、保存二维码、订单状态查询和更新 |
| Payment flow | `internal/service/wx_pay.go`、`internal/service/ali_pay.go` | 微信/支付宝下单、通知、查单、关单 |
| Refund flow | `internal/service/refund_application.go`、`internal/service/refund_info.go`、`internal/service/refund_status.go`、`internal/service/refund_status_mapping.go`、`internal/service/wx_refund.go` | 退款申请、审核、渠道提交、状态同步、订单退款状态汇总 |
| Payment audit | `internal/service/payment.go` | 写入支付流水，重复流水按幂等成功处理 |
| MQ | `internal/mq/order_close.go`、`internal/service/order_close.go` | 延迟关单消息声明、发送、消费 |
| Lock | `internal/lock/redis_lock.go` | Redis `SETNX` 分布式锁 |
| Channel clients | `internal/pay/*.go` | 微信 V2/V3、支付宝签名、验签、HTTP 调用 |
| Database | `payment-demo-go/sql/payment_demo.sql` | 初始化商品、订单、支付流水、退款表和唯一索引 |
| Frontend | `payment-demo-react/src/api/*.js`、`payment-demo-vue/src/api/*.js` | 前端 API 封装，baseURL 固定为 `http://localhost:8080` |
| Tests | `payment-demo-go/internal/**/*_characterization_test.go` | 第一批当前行为特征测试 |

### 2.3 现有可疑行为

- 现状：普通业务失败多数返回 HTTP 200 + `code=-1`。
- 现状：本地订单轮询只要不是 `支付成功`，统一返回 `code=101,message=支付中......`。
- 现状：下单按 `product_id + payment_type + payment_app_id + 未支付` 复用最近订单，没有用户维度。
- 现状：微信 V2/V3 都使用支付类型 `微信`，并共享订单 `code_url` 字段。
- 现状：`SaveCodeURL` 只在 `code_url` 为空时写入，已有值不会刷新。
- 现状：支付流水重复插入通过错误字符串包含 `Duplicate` 判断并吞掉。
- 现状：支付宝查单非成功响应返回空字符串和 nil error。
- 现状：退款申请会把待审核退款计入占用可退金额。
- 现状：支付宝退款状态只有 `REFUND_SUCCESS` 映射成本地成功，其他状态映射为空字符串。
- 现状：微信退款状态 `CHANGE` 映射为本地 `ABNORMAL`，`REFUNDCLOSE` 映射为本地 `CLOSED`。

## 3. 契约

### 3.1 普通 REST 响应

成功响应：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

失败响应：

```json
{
  "code": -1,
  "message": "失败原因",
  "data": {}
}
```

当前契约：普通 REST 错误通常仍是 HTTP 200，调用方根据 `code` 判断业务成功。

### 3.2 渠道回调响应

| 回调 | 成功响应 | 失败/拒绝响应 |
|---|---|---|
| 微信 V3 支付/退款通知 | HTTP 200 JSON：`{"code":"SUCCESS","message":"成功"}` | HTTP 500 JSON：`{"code":"ERROR","message":"失败"}` |
| 微信 V2 支付通知 | HTTP 200 XML：`SUCCESS/OK` | HTTP 200 XML：`FAIL/失败`、`FAIL/验签失败`、`FAIL/金额校验失败` |
| 支付宝支付通知 | HTTP 200 字符串 `success` | HTTP 200 字符串 `failure` |

### 3.3 状态迁移

订单状态当前用中文字符串持久化：

| 当前值 | 触发 |
|---|---|
| `未支付` | 新订单 |
| `支付成功` | 支付通知或主动查单确认成功 |
| `超时已关闭` | 延迟关单查单后确认未支付/关闭 |
| `用户已取消` | 前端取消未支付订单 |
| `退款中` | 本地退款汇总存在处理中金额 |
| `部分退款` | 成功退款金额大于 0 且小于订单金额 |
| `已退款` | 成功退款金额大于等于订单金额 |
| `退款异常` | 无成功/处理中且存在异常退款 |

退款审核状态：`PENDING`、`APPROVED`、`REJECTED`。  
退款状态：`CREATED`、`PROCESSING`、`SUCCESS`、`FAILED`、`CLOSED`、`ABNORMAL`。

## 4. 隐式输入输出 / 状态地图

| 类型 | 名称 / 位置 | 读 | 写 | 生命周期 | 备注 |
|---|---|---|---|---|---|
| DB | `t_product` | 商品列表、下单 | 初始化脚本 | 持久 | 内置课程数据 |
| DB | `t_order_info` | 列表、轮询、查单、退款、关单 | 下单、通知、取消、查单、退款汇总 | 持久 | `uk_order_no` 防重复 |
| DB | `t_payment_info` | 审计/排障 | 支付通知、主动查单同步 | 持久 | 两个唯一索引兜底幂等 |
| DB | `t_refund_info` | 退款列表、审核、对账 | 退款申请、审核、渠道同步 | 持久 | 审核状态和退款状态分离 |
| Redis | `payment:wx:notify:processed:*` | 微信 V3 通知 | 微信 V3 通知 | 24 小时 | 先 `processing`，成功后 `processed` |
| Redis | `payment:wx:v2:notify:processed:*` | 微信 V2 通知 | 微信 V2 通知 | 24 小时 | 使用 `transaction_id` |
| Redis | `payment:*` lock key | 关键业务路径 | 关键业务路径 | lease 到期或 Lua 释放 | `SETNX` + UUID |
| RabbitMQ | `payment.order.close.event.exchange` | RabbitMQ | 新订单创建 | 持久消息 | 延迟关单生产入口 |
| RabbitMQ | `payment.order.close.delay.queue` | RabbitMQ | 新订单创建 | TTL 队列 | TTL 来自配置 |
| RabbitMQ | `payment.order.close.release.queue` | 消费者 | 死信交换机 | 长期 | 延迟关单消费入口 |
| Config | `config/application.yml` | 启动期 | 人工 | 启动期 | Spring 风格配置 |
| Config | `wxpay.properties`、`alipay-sandbox.properties`、`apiclient_key.pem` | 启动期/历史订单 fallback | 人工 | 启动期 | 包含敏感支付配置，新下单使用 DB 支付应用配置 |
| Log | `log.Printf`、`log.Fatalf`、GORM logger | 运维/调试 | 启动、MQ、SQL | 运行期 | 当前没有统一审计表 |

## 5. 多套实现 / 多套规则并存

- 微信 V3 与微信 V2 并存，签名、报文、通知响应格式不同。
- 微信 Native、微信 JSAPI、支付宝电脑网站支付并存。
- React 与 Vue 两套前端并存，调用路径基本一致。
- 退款申请存在 JSON body 新入口、支付渠道兼容入口、path variable 旧入口。
- 幂等同时依赖 Redis 通知 key、Redis 锁、数据库行锁、状态条件更新、唯一索引。
- 配置文件沿用 Spring Boot 风格，当前运行时代码是 Go。

## 6. 验收标准

- [x] 当前 public HTTP 路径和响应结构被记录。
- [x] 微信 V2/V3、支付宝回调响应格式被记录。
- [x] DB、Redis、RabbitMQ、配置文件、日志/审计状态被记录。
- [x] 可疑行为被明确标为现状，不在本 spec 中修复。
- [x] 第一批特征测试已覆盖 public API、返回结构、拒绝路径、事件格式和配置隔离。
- [x] `cd payment-demo-go && go test ./...` 当前通过。

## 7. 特征测试清单

| 测试文件 | 覆盖行为 |
|---|---|
| `payment-demo-go/internal/response/response_characterization_test.go` | 普通 REST 成功/失败响应结构 |
| `payment-demo-go/internal/handler/handler_characterization_test.go` | public API、CORS、HTTP 200 错误结构、微信 V2/V3 拒绝路径 |
| `payment-demo-go/internal/config/config_characterization_test.go` | 配置环境变量覆盖、默认端口、默认关单延迟、私钥相对路径解析 |
| `payment-demo-go/internal/lock/redis_lock_characterization_test.go` | Redis 锁空 key 拒绝路径 |
| `payment-demo-go/internal/mq/order_close_characterization_test.go` | 延迟关单事件名称和消息 JSON 格式 |
| `payment-demo-go/internal/pay/wxv2_characterization_test.go` | 微信 V2 签名、XML helper 当前格式 |
| `payment-demo-go/internal/service/refund_characterization_test.go` | 退款状态映射、同步状态集合、duplicate 判断 |

## 8. 兼容性影响

- **HTTP 路径**：当前路径均为既有 public API，后续不得无 spec 直接变更。
- **响应结构**：普通 REST 的 `R{code,message,data}`、微信 XML/JSON、支付宝字符串响应是兼容面。
- **数据库结构**：`payment_demo.sql` 中表名、字段名、唯一索引影响幂等和历史数据。
- **状态值**：订单中文状态和退款英文枚举影响 DB、前端展示、状态条件更新。
- **配置字段**：`application.yml`、微信 properties、支付宝 properties 是启动期兼容面。
- **前端影响**：React/Vue 都固定请求 `http://localhost:8080`，API 路径变更需同步两端。
- **第三方平台配置**：微信/支付宝通知路径、证书、密钥、回调域名是外部配置面。

## 9. 实现锚点

- Controller / Handler：`payment-demo-go/internal/handler/handler.go`
- Service：`payment-demo-go/internal/service/*.go`
- Channel clients：`payment-demo-go/internal/pay/*.go`
- Entity / DTO / Enum：`payment-demo-go/internal/model/models.go`、`payment-demo-go/internal/constant/enums.go`
- SQL：`payment-demo-go/sql/payment_demo.sql`
- Config / MQ / Cache：`payment-demo-go/internal/config/config.go`、`payment-demo-go/internal/mq/order_close.go`、`payment-demo-go/internal/lock/redis_lock.go`
- Frontend：`payment-demo-react/src/api/*.js`、`payment-demo-vue/src/api/*.js`
- Tests：`payment-demo-go/internal/**/*_characterization_test.go`
- Docs：`CODE_INTRO.md`、`spec/README.md`

## 10. 回滚 / 观测

- **回滚方式**：代码回滚；配置或 DB 变更需按具体 spec 单独说明。
- **需要观察的日志**：启动失败日志、MQ 解析/处理失败日志、GORM SQL 日志。
- **需要观察的数据**：订单状态、支付流水、退款状态、Redis 通知 key、RabbitMQ 延迟关单队列。
- **失败时对外表现**：普通 REST 可能返回 HTTP 200 + `code=-1`；微信/支付宝回调会按渠道格式返回失败，渠道可能重试。

## 11. 变更记录

| 日期 | 状态 | 改了什么 | 关联 issue / PR |
|---|---|---|---|
| 2026-06-12 | implemented | 建立当前行为 spec，关联第一批特征测试 | 建立最小 spec 状态账本 |

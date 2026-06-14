# Payment Demo 代码导览

更新时间：2026-06-13

本文件按当前源码、`spec/README.md` 和已落地 spec 重新生成，用作进入项目时的第一份代码地图。项目主体是支付演示系统：一个 Go 后端、两套前端实现（React 与 Vue）、达梦 DM8 数据库脚本，以及围绕支付配置、订单、退款、MQ 补偿和 spec 治理的一组文档与测试。

## 1. 项目结构

```text
.
+-- AGENTS.md                         # 仓库级协作规则：issue 分类、spec、测试、完成定义
+-- CODE_INTRO.md                     # 当前文件
+-- materials/                        # 架构、测试、治理行动卡
+-- spec/                             # 项目状态账本
|   +-- README.md                     # spec 索引
|   +-- governance/                   # 治理规范
|   +-- implemented/                  # 已实现且测试锁住的行为
+-- live_spec_template/               # live spec 模板与参考
+-- payment-demo-go/                  # Go 后端
+-- payment-demo-react/               # React 18 + Vite 前端
+-- payment-demo-vue/                 # Vue 2 + Vue CLI 前端
```

`spec/` 不是一次性文档，而是项目状态账本。新增功能、公共接口、状态机、DB、MQ、配置或兼容变化，都要先查 `spec/README.md`，必要时先补 `spec/planned/`，实现完成后再进入 `spec/implemented/`。

当前关键 implemented spec：

| 领域 | 文件 | 说明 |
|---|---|---|
| current behavior | `spec/implemented/current-behavior/PAYMENT_DEMO_GO_CURRENT_BEHAVIOR_SPEC.md` | Go 后端当前行为快照 |
| database | `spec/implemented/database/DAMENG_DATABASE_MIGRATION_SPEC.md` | 默认数据库迁移到达梦 DM8 |
| payment config | `spec/implemented/payment-config/PAYMENT_CONFIG_MANAGEMENT_SPEC.md` | 支付渠道和支付应用配置管理 |
| payment config | `spec/implemented/payment-config/REQUIRED_PAYMENT_APP_ID_ORDER_BINDING_SPEC.md` | 新订单强制绑定支付应用 ID |
| payment config | `spec/implemented/payment-config/PAYMENT_APP_ONLY_FRONTEND_SELECTION_SPEC.md` | 前端只展示支付应用，不再单独展示支付方式 |
| refund | `spec/implemented/refund/REFUND_AUTO_QUERY_AFTER_APPROVAL_SPEC.md` | 退款审核后通过 RabbitMQ 自动查询退款结果 |

## 2. 后端总览

后端目录：`payment-demo-go/`

技术栈：

- Go + Gin
- GORM + 达梦 DM8 driver：`github.com/godoes/gorm-dameng`
- Redis：通知幂等、分布式锁
- RabbitMQ：延迟关单、退款自动查询
- 微信支付 V3/V2、支付宝沙箱
- 特征测试和回归测试：`go test ./...`

主要目录：

```text
payment-demo-go/
+-- cmd/server/main.go                # 启动入口
+-- config/application.yml            # 运行配置
+-- config/alipay-sandbox.properties  # 支付宝沙箱配置材料
+-- config/apiclient_key.pem          # 微信支付私钥文件
+-- docker-compose.yml                # DM8 容器示例
+-- internal/
|   +-- config/                       # yml 加载、DM DSN 转换
|   +-- constant/                     # 订单、退款、支付类型、渠道状态常量
|   +-- db/                           # GORM 达梦连接
|   +-- handler/                      # Gin HTTP 路由
|   +-- lock/                         # Redis 分布式锁
|   +-- model/                        # GORM 模型与请求 DTO
|   +-- mq/                           # RabbitMQ 声明、生产、消费
|   +-- pay/                          # 微信/支付宝底层客户端
|   +-- response/                     # 统一响应结构
|   +-- service/                      # 业务服务
|   +-- types/                        # ID、时间 JSON 类型
|   +-- util/                         # 单号、金额、JSON、业务错误工具
+-- sql/payment_demo.sql              # 达梦初始化脚本
```

启动链路在 `cmd/server/main.go`：

1. 读取 `config/application.yml`。
2. 连接达梦数据库。
3. 连接 Redis 并 `PING`。
4. 连接 RabbitMQ，声明订单关单和退款查询两组队列。
5. 初始化微信 V3/V2 和支付宝客户端。
6. 构造 `service.Service`。
7. 启动订单延迟关单消费者。
8. 启动退款自动查询消费者。
9. 注册 Gin 路由并监听 `server.port`，当前配置为 `8080`。

## 3. 配置与运行依赖

核心配置文件：`payment-demo-go/config/application.yml`

当前默认依赖：

| 依赖 | 当前配置重点 |
|---|---|
| HTTP | `server.port: 8080` |
| 达梦 | `jdbc:dm://192.168.220.200:5236?schema=PAYMENT_DEMO&connectTimeout=30000` |
| Redis | `192.168.220.200:6379` |
| RabbitMQ | `amqp://guest:guest@192.168.220.200:5672/` |
| 订单延迟关单 | `payment.order.close-delay-ms: 60000` |

达梦 DSN 注意点：

- 配置里保留 Spring 风格字段：`spring.datasource.url`、`username`、`password`。
- Go 配置加载会把 `jdbc:dm://...` 转成达梦 Go driver 使用的 `dm://username:password@host:port?...`。
- 当前密码包含 `#`、`@` 等特殊字符，DSN 拼接保持原样，避免 URL encode 后 driver 登录失败。
- 默认 schema 是 `PAYMENT_DEMO`。如果数据库还没有这个 schema，要先执行 `payment-demo-go/sql/payment_demo.sql`。

`docker-compose.yml` 提供 DM8 容器示例，镜像为 `registry.cn-hangzhou.aliyuncs.com/snow-io/dm8:latest`，端口映射 `5236:5236`，默认 `SYSDBA_PWD` 与当前配置一致。

## 4. 数据库

初始化脚本：`payment-demo-go/sql/payment_demo.sql`

当前脚本是达梦方言：

- 通过匿名块检测并创建 `PAYMENT_DEMO` schema。
- `SET SCHEMA PAYMENT_DEMO` 后创建业务表。
- 使用 `IDENTITY(1,1)`、`CLOB`、普通索引和唯一索引。
- 不再使用 MySQL 专属语法，例如 `AUTO_INCREMENT`、反引号、`ENGINE`、`CHARSET`、`ON UPDATE CURRENT_TIMESTAMP`。
- `update_time` 由 GORM `autoUpdateTime` 负责刷新。

核心表：

| 表 | 说明 |
|---|---|
| `t_product` | 演示商品，脚本内置 4 条课程数据 |
| `t_order_info` | 订单主表，包含 `payment_type` 与可空 `payment_app_id` |
| `t_payment_info` | 支付流水和第三方交易结果 |
| `t_payment_channel` | 支付渠道级配置，例如 `wxpay`、`alipay` |
| `t_payment_app` | 支付应用/商户级配置，前端现在直接选择此表数据 |
| `t_refund_info` | 退款申请、审批、渠道退款状态、通知内容 |

重要索引和幂等约束：

- `t_order_info.order_no` 唯一。
- `t_payment_info(order_no, payment_type)` 唯一。
- `t_payment_info(transaction_id, payment_type)` 唯一。
- `t_refund_info.refund_no` 唯一。
- `t_refund_info.refund_id` 唯一。

## 5. 领域模型与状态

模型文件：`payment-demo-go/internal/model/models.go`

主要模型：

- `OrderInfo`
- `PaymentInfo`
- `Product`
- `PaymentChannel`
- `PaymentApp`
- `RefundInfo`
- `OrderCloseMessage`
- `RefundQueryMessage`

订单状态定义在 `internal/constant/enums.go`：

| 状态 | 含义 |
|---|---|
| `未支付` | 新建或待支付 |
| `支付成功` | 支付完成 |
| `超时已关闭` | 延迟关单后关闭 |
| `用户已取消` | 用户主动取消 |
| `退款中` | 有退款处理中 |
| `部分退款` | 已成功部分退款 |
| `已退款` | 已全额退款 |
| `退款异常` | 存在异常退款 |

支付类型：

- `微信`
- `支付宝`

退款审批状态：

- `PENDING`
- `APPROVED`
- `REJECTED`

退款执行状态：

- `CREATED`
- `PROCESSING`
- `SUCCESS`
- `FAILED`
- `CLOSED`
- `ABNORMAL`

## 6. 支付配置模型

支付配置分两层：

1. `t_payment_channel`：渠道级配置，例如渠道 code、渠道名、支付类型、启用状态、公共 JSON 参数。
2. `t_payment_app`：应用/商户级配置，例如应用 code、应用名、所属渠道、支付类型、启用状态、应用 JSON 参数。

配置合并顺序：

```text
application.yml 基础配置
  < t_payment_channel.config_params
  < t_payment_app.app_config
```

关键规则：

- 新下单接口必须传 `paymentAppId`，且必须大于 0。
- 新订单会校验支付应用存在、启用，并且所属渠道也启用。
- 下单接口的支付类型必须与支付应用的 `payment_type` 匹配。
- 订单会持久化 `payment_app_id`，后续查询、取消、退款、通知处理会校验订单与应用绑定关系。
- 历史订单如果 `payment_app_id` 为空，仍回退到 `application.yml` 中的基础配置。
- 微信通知通过 `appid/mch_id` 匹配支付应用；支付宝通知通过 `app_id/seller_id` 匹配支付应用。

前端当前只展示支付应用列表，不再单独展示“微信/支付宝”支付方式。用户选择支付应用后，前端按 `paymentType` 或 `channelCode` 推断调用微信或支付宝下单接口。

## 7. HTTP API

路由入口：`payment-demo-go/internal/handler/handler.go`

统一响应：

- 常规业务接口大多返回 HTTP 200。
- 成功通常是 `code: 0`。
- 业务失败通过统一响应结构返回非 0 code 与 message。
- 微信/支付宝第三方通知接口按第三方要求返回 JSON、XML 或纯文本。

### 商品与订单

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/product/test` | 测试接口 |
| GET | `/api/product/list` | 商品列表，返回 `productList` |
| GET | `/api/order-info/list` | 订单列表，返回 `list` |
| GET | `/api/order-info/query-order-status/:orderNo` | 前端轮询支付结果；支付成功返回 `code: 0`，未成功返回 `code: 101` |

### 支付渠道配置

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/payment-channel/list` | 渠道列表 |
| POST | `/api/payment-channel/save` | 新增渠道 |
| POST | `/api/payment-channel/update/:channelCode` | 更新渠道 |
| POST | `/api/payment-channel/delete/:channelCode` | 删除渠道 |

注意：当前实现与前端使用 POST 做 update/delete，不是 PUT/DELETE。

### 支付应用配置

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/payment-app/list` | 应用列表；支持 `channelCode`、`paymentType`、`enabled` 查询参数 |
| POST | `/api/payment-app/save` | 新增应用 |
| POST | `/api/payment-app/update/:appCode` | 更新应用 |
| POST | `/api/payment-app/delete/:appCode` | 删除应用 |
| GET | `/api/payment-config/apps` | 首页使用的已启用支付应用列表，返回 `apps` |
| POST | `/api/payment-config/reload` | 当前等价于重新返回启用应用列表 |

### 微信支付

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/wx-pay/native/:productId?paymentAppId=...` | 微信 V3 Native 下单 |
| POST | `/api/wx-pay/native/notify` | 微信 V3 支付通知 |
| POST | `/api/wx-pay/cancel/:orderNo` | 微信取消/关闭订单 |
| GET | `/api/wx-pay/query/:orderNo` | 微信订单查询 |
| GET | `/api/wx-pay/check-order-status/:orderNo` | 微信订单状态检查 |
| GET | `/api/wx-pay/query-refund/:refundNo` | 微信退款查询 |
| POST | `/api/wx-pay/refunds/notify` | 微信退款通知 |
| GET | `/api/wx-pay/querybill/:billDate/:type` | 微信账单 URL 查询 |
| GET | `/api/wx-pay/downloadbill/:billDate/:type` | 微信账单下载 |
| POST | `/api/wx-pay/jsapi` | 微信 JSAPI 下单 |
| POST | `/api/wx-pay/jsapi/notify/v1` | JSAPI 通知入口，复用 V3 通知处理 |
| POST | `/api/wx-pay-v2/native/:productId?paymentAppId=...` | 微信 V2 Native 下单 |
| POST | `/api/wx-pay-v2/native/notify` | 微信 V2 支付通知 |

### 支付宝

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/ali-pay/trade/page/pay/:productId?paymentAppId=...` | 支付宝电脑网站支付下单，返回 `formStr` |
| POST | `/api/ali-pay/trade/notify` | 支付宝支付通知 |
| POST | `/api/ali-pay/trade/close/:orderNo` | 支付宝关闭订单 |
| GET | `/api/ali-pay/trade/query/:orderNo` | 支付宝订单查询 |
| GET | `/api/ali-pay/trade/fastpay/refund/:refundNo` | 支付宝退款查询 |
| GET | `/api/ali-pay/bill/downloadurl/query/:billDate/:type` | 支付宝账单下载地址查询 |

### 退款

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/refund-info/apply` | 创建退款申请，JSON body：`orderNo`、`refundAmount`、`reason` |
| POST | `/api/wx-pay/refunds` | 兼容入口，创建退款申请 |
| POST | `/api/ali-pay/trade/refund` | 兼容入口，创建退款申请 |
| POST | `/api/refund-info/apply/:orderNo/:reason` | 旧版兼容申请入口 |
| POST | `/api/wx-pay/refunds/:orderNo/:reason` | 旧版兼容申请入口 |
| POST | `/api/ali-pay/trade/refund/:orderNo/:reason` | 旧版兼容申请入口 |
| GET | `/api/refund-info/list` | 全部退款申请 |
| GET | `/api/refund-info/list/:orderNo` | 指定订单退款申请 |
| POST | `/api/refund-info/approve/:refundNo` | 审核通过并提交渠道退款 |
| POST | `/api/refund-info/reject/:refundNo` | 审核拒绝 |
| POST | `/api/refund-info/query/:refundNo` | 手动查询并同步渠道退款状态 |
| POST | `/api/refund-info/reconcile/:orderNo` | 订单维度退款对账 |

## 8. 订单流程

核心文件：

- `internal/service/order.go`
- `internal/service/payment.go`
- `internal/service/wx_pay.go`
- `internal/service/ali_pay.go`
- `internal/service/order_close.go`

下单行为：

1. 前端选择商品与支付应用。
2. 前端根据支付应用推断微信或支付宝下单接口。
3. 后端校验 `paymentAppId`、支付应用、支付渠道和支付类型匹配。
4. 后端按 `product_id + payment_type + payment_app_id + 未支付` 查找可复用订单。
5. 如果没有可复用订单，则创建新订单并写入 `payment_app_id`。
6. 新订单创建后投递订单延迟关单消息。
7. 微信 Native 返回二维码链接；支付宝返回自动提交表单字符串。
8. 前端轮询 `/api/order-info/query-order-status/:orderNo`，成功后跳转成功页。

订单关单补偿：

- 新订单创建后投递 `OrderCloseMessage`。
- 延迟队列到期后消费者只处理仍为 `未支付` 的订单。
- 微信订单走 `WxCheckOrderStatus`。
- 支付宝订单走 `AliCheckOrderStatus`。

## 9. 退款流程

核心文件：

- `internal/service/refund_info.go`
- `internal/service/refund_application.go`
- `internal/service/refund_auto_query.go`
- `internal/service/refund_status.go`
- `internal/service/refund_status_mapping.go`
- `internal/service/wx_refund.go`

退款主链路：

1. 前端或接口创建退款申请，状态为 `PENDING + CREATED`。
2. 审核拒绝会标记为 `REJECTED + CLOSED`。
3. 审核通过后，后端先标记审批通过，再抢占退款执行权。
4. 后端按订单 `payment_type` 调用微信或支付宝退款能力。
5. 提交渠道成功后，本地通常进入 `PROCESSING` 或终态。
6. 后端投递退款自动查询延迟消息。
7. 渠道通知、自动查询、手动查询、订单对账都会复用统一同步逻辑刷新退款和订单汇总状态。

退款自动查询：

- 消息体：`{"refundNo":"...","attempt":1}`。
- 最大自动查询次数：5。
- 延迟间隔：1 分钟。
- 只有审批通过且退款状态仍为 `CREATED` 或 `PROCESSING` 时继续查询。
- `SUCCESS`、`FAILED`、`CLOSED`、`ABNORMAL` 均视为终态，不再自动重试。
- 查询失败不会立即 RabbitMQ 热重回，而是按剩余次数重新投递延迟消息。
- 超过次数后保留手动查询和订单对账接口兜底。

## 10. RabbitMQ

MQ 客户端：`payment-demo-go/internal/mq/`

启动时会声明两组 TTL + 死信交换机延迟队列。

### 订单延迟关单

| 名称 | 值 |
|---|---|
| event exchange | `payment.order.close.event.exchange` |
| dead-letter exchange | `payment.order.close.dead-letter.exchange` |
| delay queue | `payment.order.close.delay.queue` |
| release queue | `payment.order.close.release.queue` |
| delay routing key | `payment.order.close.delay` |
| release routing key | `payment.order.close.release` |

消息：

```json
{"orderNo":"...","paymentType":"微信"}
```

TTL 使用配置 `payment.order.close-delay-ms`。

### 退款自动查询

| 名称 | 值 |
|---|---|
| event exchange | `payment.refund.query.event.exchange` |
| dead-letter exchange | `payment.refund.query.dead-letter.exchange` |
| delay queue | `payment.refund.query.delay.queue` |
| release queue | `payment.refund.query.release.queue` |
| delay routing key | `payment.refund.query.delay` |
| release routing key | `payment.refund.query.release` |

消息：

```json
{"refundNo":"...","attempt":1}
```

TTL 固定为 1 分钟。

## 11. Redis

Redis 主要用途：

- 分布式锁：订单创建、退款审核、退款查询、退款对账。
- 微信 V3 通知幂等 key：`payment:wx:notify:processed:<notifyId>`。
- 微信 V2 通知幂等 key：`payment:wx:v2:notify:processed:<transactionId>`。

通知幂等 key 默认保留 24 小时。处理失败时会释放 `processing` 标记，让第三方后续重试能够再次处理。

## 12. React 前端

目录：`payment-demo-react/`

技术栈：

- React 18
- Vite 5
- React Router 6
- Ant Design 5
- Axios
- `qrcode.react`

入口与路由：

- `src/main.jsx`
- `src/App.jsx`

页面：

| 路由 | 文件 | 说明 |
|---|---|---|
| `/` | `src/pages/Home.jsx` | 商品列表、支付应用选择、下单 |
| `/orders` | `src/pages/Orders.jsx` | 订单与退款管理 |
| `/download` | `src/pages/Download.jsx` | 账单查询/下载 |
| `/success` | `src/pages/Success.jsx` | 支付成功页 |

API 封装：

```text
src/api/product.js
src/api/orderInfo.js
src/api/wxPay.js
src/api/aliPay.js
src/api/bill.js
src/api/refundInfo.js
src/api/paymentConfig.js
src/api/paymentChannel.js
src/api/paymentApp.js
```

首页当前行为：

- 加载商品列表：`GET /api/product/list`。
- 加载启用支付应用：`GET /api/payment-config/apps`。
- 默认选中第一个可用支付应用。
- 不再维护独立 `payType`。
- 微信应用调用微信 V3 Native 下单。
- 支付宝应用调用支付宝电脑网站支付。
- 微信 V2 按钮只允许微信支付应用使用。

常用命令：

```bash
cd payment-demo-react
npm install
npm run dev
npm run build
```

开发服务默认端口：`3000`。

## 13. Vue 前端

目录：`payment-demo-vue/`

技术栈：

- Vue 2.6
- Vue CLI 4
- Vue Router 3
- Element UI 2
- Axios
- `vue-qriously`

入口与路由：

- `src/main.js`
- `src/router/index.js`

页面：

| 路由 | 文件 | 说明 |
|---|---|---|
| `/` | `src/views/index.vue` | 商品列表、支付应用选择、下单 |
| `/orders` | `src/views/Orders.vue` | 订单与退款管理 |
| `/download` | `src/views/Download.vue` | 账单查询/下载 |
| `/success` | `src/views/Success.vue` | 支付成功页 |

Vue 首页与 React 首页保持同一业务契约：

- 只展示支付应用。
- 用支付应用推断渠道。
- 下单请求带 `paymentAppId`。
- 微信 V2 只允许微信应用。

常用命令：

```bash
cd payment-demo-vue
npm install
npm run serve
npm run build
```

开发服务默认端口通常为 `8080`，如果 Go 后端已占用 8080，需要前端 CLI 自动换端口或手动指定其他端口。

## 14. 测试与验证

后端基础验证：

```bash
cd payment-demo-go
go test ./...
```

前端构建验证：

```bash
cd payment-demo-react
npm run build

cd payment-demo-vue
npm run build
```

当前前端构建可能出现 bundle size warning，这不是构建失败。

测试覆盖重点：

- 配置加载和达梦 DSN 转换。
- 达梦 GORM dialector。
- 达梦 SQL 初始化脚本特征。
- 统一响应结构。
- 支付配置管理和 `paymentAppId` 校验。
- 支付应用与订单绑定。
- RabbitMQ exchange、queue、routing key 名称。
- 退款自动查询重试边界。
- Redis 锁行为。
- Handler 路由与关键响应契约。

## 15. 维护注意事项

- 任何涉及 HTTP 路径、响应结构、状态值、DB schema、配置字段、MQ 名称、前端调用契约、第三方回调响应的变化，都属于公共接口或兼容影响，必须先更新 spec。
- 行为变更要先补能失败的测试，再做最小实现。
- DB、Redis、RabbitMQ、配置文件、环境变量或全局状态相关测试要隔离并 reset。
- 支付渠道回调、错误/拒绝路径、事件消息、日志/审计字段都属于契约面，改动时要补测试。
- 前后端 API 封装要与 `handler.go` 保持同步，尤其注意当前配置 update/delete 接口是 POST。
- 新订单已经要求 `paymentAppId`，前端不要重新引入独立支付方式选择。
- 退款审核通过后必须保证渠道退款提交成功才投递自动查询消息；如果投递失败，审核接口应返回错误，避免形成无人兜底的处理中退款。
- 达梦 schema、脚本、配置和 README 要一起维护，避免再次出现 schema 不存在或账号密码解析错误。

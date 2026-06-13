# 支付参数管理与支付应用管理 SPEC

## 0. 元信息

- 状态：implemented
- 领域：payment-config
- 更新日期：2026-06-13
- 关联任务：支付参数管理、支付应用管理、下单 paymentAppId 参数、支付应用 ID 必填与订单绑定配置
- 影响范围：backend | react-frontend | vue-frontend | database | tests

## 1. 目标

系统支持通过数据库维护支付渠道和支付应用配置。微信 V3、微信 V2、支付宝下单接口要求提供 `paymentAppId` 参数；系统按支付应用和支付渠道配置创建支付入口，并将支付应用 ID 绑定到订单。

## 2. 契约

### 支付渠道管理

| API | 方法 | 响应数据 |
|---|---|---|
| `/api/payment-channel/list` | GET | `data.list` |
| `/api/payment-channel/save` | POST | `data.paymentChannel` |
| `/api/payment-channel/update/:channelCode` | POST | `data.paymentChannel` |
| `/api/payment-channel/delete/:channelCode` | POST | 普通成功响应 |

请求字段：`channelCode`、`channelName`、`paymentType`、`enabled`、`configParams`。

### 支付应用管理

| API | 方法 | 响应数据 |
|---|---|---|
| `/api/payment-app/list` | GET | `data.list` |
| `/api/payment-app/save` | POST | `data.paymentApp` |
| `/api/payment-app/update/:appCode` | POST | `data.paymentApp` |
| `/api/payment-app/delete/:appCode` | POST | 普通成功响应 |

请求字段：`appCode`、`appName`、`channelCode`、`paymentType`、`enabled`、`appConfig`。

`/api/payment-app/list` 支持查询参数：`channelCode`、`paymentType`、`enabled`。

### 支付配置入口

| API | 方法 | 响应数据 |
|---|---|---|
| `/api/payment-config/apps` | GET | `data.apps` |
| `/api/payment-config/reload` | POST | `data.apps` |

当前实现没有长期内存缓存，`reload` 返回数据库中的启用应用列表，作为后续缓存实现的兼容入口。

### 下单参数

以下接口要求 query 参数 `paymentAppId`：

- `POST /api/wx-pay/native/:productId?paymentAppId=1`
- `POST /api/wx-pay-v2/native/:productId?paymentAppId=1`
- `POST /api/ali-pay/trade/page/pay/:productId?paymentAppId=2`

规则：

- 未传 `paymentAppId`：返回现有 REST 错误结构 `code=-1`。
- 传入 `paymentAppId`：读取 `t_payment_app` 中匹配支付类型且启用的应用配置，并要求所属支付渠道启用，用于本次下单。
- 配置合并顺序：properties 默认配置 < `t_payment_channel.config_params` < `t_payment_app.app_config`。
- 应用不存在、禁用、支付类型不匹配，或 `paymentAppId` 非正整数：返回现有 REST 错误结构 `code=-1`。
- 新订单记录 `t_order_info.payment_app_id`；历史空值订单的后续操作继续允许回退 properties 默认配置。
- 未支付订单复用按 `productId + paymentType + paymentAppId` 隔离。
- 取消、查单、延迟关单、退款提交/查询/对账和支付回调校验使用订单绑定的支付应用配置。

## 3. 验收标准

- 支付渠道 list/save/update/delete 接口可用。
- 支付应用 list/save/update/delete 接口可用。
- `/api/payment-config/apps` 与 `/api/payment-config/reload` 返回启用应用列表。
- 微信 V3、微信 V2、支付宝下单要求 `paymentAppId`。
- 不传 `paymentAppId` 时返回 `code=-1`。
- 后续支付生命周期使用订单绑定的支付应用配置。
- 传入不存在、禁用或类型不匹配应用时返回 `code=-1`。
- 订单模型和初始化 SQL 包含 `payment_app_id`。
- `cd payment-demo-go && go test ./...` 通过。

## 4. 实现锚点

- Handler：`payment-demo-go/internal/handler/handler.go`
- Service：`payment-demo-go/internal/service/payment_config.go`
- Payment flow：`payment-demo-go/internal/service/wx_pay.go`、`payment-demo-go/internal/service/ali_pay.go`
- Order flow：`payment-demo-go/internal/service/order.go`
- Model：`payment-demo-go/internal/model/models.go`
- SQL：`payment-demo-go/sql/payment_demo.sql`
- Frontend API：`payment-demo-react/src/api/*.js`、`payment-demo-vue/src/api/*.js`
- Tests：`payment-demo-go/internal/service/payment_config_characterization_test.go`、`payment-demo-go/internal/handler/handler_characterization_test.go`、`payment-demo-go/internal/model/payment_config_characterization_test.go`、`payment-demo-go/sql/payment_demo_sql_characterization_test.go`

## 5. 兼容影响

- 新增接口，不删除旧接口。
- 原下单路径不变，`paymentAppId` 从可选参数变为必填参数。
- 原 Go service 方法 `WxNativePay`、`WxNativePayV2`、`AliTradeCreate`、`CreateOrReuseOrder` 保留，但新下单缺少应用 id 会返回业务错误。
- REST 响应结构保持现有 `code/message/data`。
- 渠道回调路径和响应格式不变。

## 6. 变更记录

| 日期 | 状态 | 内容 |
|---|---|---|
| 2026-06-13 | implemented | 新增支付渠道/应用管理接口、下单应用参数、SQL/模型/前端 API 包装与特征测试 |
| 2026-06-13 | implemented | 下单 paymentAppId 改为必填，并将订单后续支付生命周期绑定到支付应用配置 |

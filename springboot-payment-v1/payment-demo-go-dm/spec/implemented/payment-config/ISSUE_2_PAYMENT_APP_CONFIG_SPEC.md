# Issue 2 支付渠道与支付应用配置入库 SPEC

## 0. 元信息

- **状态**：implemented
- **领域（feature-domain）**：payment-config
- **更新于**：2026-06-12
- **负责人**：待定
- **关联 issue / PR / 任务**：Refs #2
- **影响范围**：backend | database | docs | tests

## 1. 背景 / 目标

GitHub issue #2 指出：支付宝、微信参数信息没有保存到数据库，也没有支付应用 app 的配置。

本 spec 锁住本次最小修复范围：在数据库初始化脚本中增加支付渠道配置表和支付应用配置表，并提供微信、支付宝的初始配置数据；在 Go model 层增加对应 GORM 映射，便于后续服务读取。

本次不改变运行时配置来源，不新增 HTTP API，不改变微信/支付宝下单、通知、退款流程。

## 2. 契约

### 2.1 数据库表

| 表 | 作用 | 关键字段 |
|---|---|---|
| `t_payment_channel` | 保存支付渠道级配置 | `channel_code`、`channel_name`、`payment_type`、`enabled`、`config_params` |
| `t_payment_app` | 保存支付应用/商户配置 | `app_code`、`app_name`、`channel_code`、`payment_type`、`enabled`、`app_config` |

唯一约束：

- `t_payment_channel.uk_channel_code`
- `t_payment_app.uk_app_code`

### 2.2 初始数据

初始化脚本必须包含：

- 微信支付渠道：`channel_code=wxpay`，`payment_type=微信`
- 微信默认应用：`app_code=wxpay-default`，包含 `appid`、`mchId`、`mchSerialNo`、`privateKeyPath`、`apiV3Key`、`partnerKey`、`domain`、`notifyDomain`
- 支付宝渠道：`channel_code=alipay`，`payment_type=支付宝`
- 支付宝沙箱应用：`app_code=alipay-sandbox`，包含 `appId`、`sellerId`、`gatewayUrl`、`merchantPrivateKey`、`alipayPublicKey`、`contentKey`、`returnUrl`、`notifyUrl`

### 2.3 Go 模型

| 模型 | 表名 |
|---|---|
| `model.PaymentChannel` | `t_payment_channel` |
| `model.PaymentApp` | `t_payment_app` |

## 3. 实现锚点

- SQL：`payment-demo-go/sql/payment_demo.sql`
- Model：`payment-demo-go/internal/model/models.go`
- Tests：`payment-demo-go/sql/payment_demo_sql_characterization_test.go`
- Tests：`payment-demo-go/internal/model/payment_config_characterization_test.go`

## 4. 兼容影响

- HTTP API：无变化。
- 前端：无变化。
- 运行时配置来源：无变化，当前仍读取 properties。
- 数据库初始化：新增两张表和两组初始配置数据。
- 外部支付渠道：无直接调用行为变化。

## 5. 验收标准

- [x] `payment_demo.sql` 定义 `t_payment_channel`。
- [x] `payment_demo.sql` 定义 `t_payment_app`。
- [x] 初始化脚本包含微信支付渠道和默认应用配置。
- [x] 初始化脚本包含支付宝渠道和沙箱应用配置。
- [x] Go model 层存在 `PaymentChannel` 和 `PaymentApp`，表名映射正确。
- [x] `cd payment-demo-go && go test ./...` 通过。

## 6. 变更记录

| 日期 | 状态 | 改了什么 | 关联 issue / PR |
|---|---|---|---|
| 2026-06-12 | implemented | 新增支付渠道/支付应用 DB 初始化和模型映射 | Refs #2 |

# 支付应用单选入口 SPEC

## 0. 元信息

- **状态**：implemented
- **日期**：2026-06-13
- **领域**：payment-config | frontend | order
- **变更类型**：前端调用契约调整、支付入口交互变更
- **目标**：React/Vue 首页不再单独列出支付方式，只展示可用支付应用；用户选择支付应用后，前端根据应用自身的 `paymentType` / `channelCode` 调用对应下单接口。

## 1. 背景

当前下单接口已经要求 `paymentAppId`。后端可以通过 `t_payment_app.id` 查询支付应用，并进一步拿到所属支付渠道配置和应用配置，所以前端无需先让用户选择“微信/支付宝”再过滤支付应用。

现状两套首页仍有两级选择：

1. 选择支付方式：微信支付 / 支付宝
2. 选择该支付方式下的支付应用

这会让 `paymentType/channelCode` 成为重复输入源。正确入口应是“支付应用即支付入口”。

## 2. 目标行为

- React 首页和 Vue 首页只展示 `GET /api/payment-config/apps` 返回的启用支付应用。
- 默认选中第一条可用支付应用。
- 切换支付应用时只更新 `paymentAppId`，不维护独立 `payType`。
- 点击“确认支付”时，从选中支付应用判断调用：
  - `paymentType=微信` 或 `channelCode=wxpay`：调用 `POST /api/wx-pay/native/:productId?paymentAppId=...`
  - `paymentType=支付宝` 或 `channelCode=alipay`：调用 `POST /api/ali-pay/trade/page/pay/:productId?paymentAppId=...`
- 点击“微信 V2 支付”时，仅当选中应用为微信应用才允许调用 `POST /api/wx-pay-v2/native/:productId?paymentAppId=...`。
- 支付应用卡片应展示应用名称、应用编码、ID、支付类型，不再展示独立支付方式卡片。
- 无可用支付应用时，支付按钮不可用并提示“请选择支付应用”。

## 3. 非目标

- 不新增 HTTP API。
- 不改后端下单路径。
- 不改 `paymentAppId` 必填规则。
- 不改 DB schema、支付渠道表、支付应用表。
- 不改订单列表中的支付方式展示；订单仍然持久化 `payment_type`，用于取消、查询、退款等后续流程。

## 4. 兼容影响

- **前端交互兼容**：首页去掉“选择支付方式”区域；用户改为直接选择支付应用。
- **HTTP 兼容**：请求路径和 query 参数不变。
- **后端兼容**：后端仍校验 `paymentAppId` 是否匹配目标下单接口的支付类型；前端根据应用推导接口只是减少无效调用。

## 5. 验收标准

- [x] React 首页源码不再出现“选择支付方式”，不再维护独立 `payType` 状态。
- [x] Vue 首页源码不再出现“选择支付方式”，不再维护独立 `payType` 状态。
- [x] React/Vue 首页均从选中支付应用推导微信 V3 / 支付宝下单接口。
- [x] React/Vue 首页微信 V2 支付仅允许微信支付应用。
- [x] React/Vue 构建通过。
- [x] `cd payment-demo-go && go test ./...` 通过。

## 6. 实现锚点

- `payment-demo-react/src/pages/Home.jsx`
- `payment-demo-vue/src/views/index.vue`
- `spec/implemented/payment-config/REQUIRED_PAYMENT_APP_ID_ORDER_BINDING_SPEC.md`
- `spec/implemented/payment-config/PAYMENT_CONFIG_MANAGEMENT_SPEC.md`
- `spec/implemented/current-behavior/PAYMENT_DEMO_GO_CURRENT_BEHAVIOR_SPEC.md`

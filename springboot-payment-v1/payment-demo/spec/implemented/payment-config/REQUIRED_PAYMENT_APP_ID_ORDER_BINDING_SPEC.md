# 支付应用 ID 必填与订单绑定配置 SPEC

## 0. 元信息

- 状态：implemented
- 领域：payment-config
- 更新日期：2026-06-13
- 影响范围：backend | react-frontend | vue-frontend | payment-provider-callback | tests | docs

## 1. 目标

下单必须显式提供 `t_payment_app.id` 作为 `paymentAppId`。系统按支付应用和支付渠道配置创建支付入口，并将应用 ID 绑定到订单；订单后续取消、查单、关单、退款和回调校验使用订单绑定的应用配置。

## 2. 契约

以下接口保留原路径，但 `paymentAppId` 变为必填 query 参数：

- `POST /api/wx-pay/native/:productId?paymentAppId=1`
- `POST /api/wx-pay-v2/native/:productId?paymentAppId=1`
- `POST /api/ali-pay/trade/page/pay/:productId?paymentAppId=2`

规则：

- 缺少、非数字或非正数 `paymentAppId`：返回现有 REST 错误结构 `HTTP 200 + code=-1`。
- `paymentAppId` 必须匹配当前支付类型、支付应用启用、所属支付渠道启用。
- 配置合并顺序：properties 默认配置 < `t_payment_channel.config_params` < `t_payment_app.app_config`。
- 新订单必须写入 `t_order_info.payment_app_id`；未支付订单复用按 `productId + paymentType + paymentAppId` 隔离。
- 历史订单 `payment_app_id` 为空时，仅后续操作允许回退 properties 默认配置；新下单不再允许空值。
- 微信 V2 回调按通知 `appid/mch_id` 选择应用验签；支付宝回调按 `app_id/seller_id` 选择应用验签；微信 V3 回调用已配置微信应用尝试验签/解密，再校验订单绑定应用。
- React/Vue 首页展示当前支付方式可用的支付应用，并把用户选择的应用 ID 传给下单接口。

## 3. 验收标准

- [x] 三条下单接口缺少 `paymentAppId` 均返回 `code=-1`。
- [x] 禁用应用、支付类型不匹配、所属渠道禁用时不能下单。
- [x] 渠道级配置和应用级配置按顺序覆盖 properties 默认配置。
- [x] 新订单记录 `payment_app_id`，不同应用的同商品未支付订单互不复用。
- [x] 取消、查单、延迟关单、退款提交/查询/对账和回调校验使用订单绑定应用配置。
- [x] React/Vue 首页可选择支付应用；无可用应用时不能发起下单。
- [x] `cd payment-demo-go && go test ./...` 通过。

## 4. 兼容影响

- HTTP 路径不变，但下单 query 参数 `paymentAppId` 从可选变必填。
- REST 响应结构不变。
- `t_order_info.payment_app_id` 继续允许 NULL，以兼容历史订单。
- properties 配置仍作为历史订单和配置缺省字段的 fallback，不再作为新下单的默认应用。

## 5. 变更记录

| 日期 | 状态 | 内容 |
|---|---|---|
| 2026-06-13 | implemented | 下单必填支付应用 ID，并将订单后续支付生命周期绑定到该应用配置 |


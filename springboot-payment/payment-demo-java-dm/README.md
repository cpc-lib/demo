# 支付渠道配置 / 支付应用配置改造说明

## 1. 改造目标

本次改造基于项目现有微信支付、支付宝支付实现完成，不使用 mock，不新增虚假支付流程。支付参数从数据库加载，前端提供配置维护页面，后端支付下单、查单、关单、退款、通知校验按订单绑定的支付应用选择真实配置。

## 2. 数据库落地

完整初始化脚本：`payment-demo-v4/sql/payment_demo_v1.sql`

增量升级脚本：`payment-demo-v4/sql/payment_config_upgrade.sql`（保留已有业务数据，仅新增配置表和订单字段）

新增核心表：

- `t_payment_channel`：支付渠道公共配置，例如 `WXPAY`、`ALIPAY`、网关地址、通知域名等。
- `t_payment_app`：渠道下的商户/应用配置，例如微信 `appid/mchId/mchSerialNo/privateKeyPath/apiV3Key/partnerKey`，支付宝 `appId/sellerId/merchantPrivateKey/alipayPublicKey/returnUrl/notifyUrl`。

订单表新增字段：

- `payment_app_id`：订单绑定的支付应用 ID。
- `payment_channel_code`：订单绑定的渠道编码。

初始化数据来源：项目原有 `wxpay.properties`、`alipay-sandbox.properties`，不是 mock 参数。

## 3. 后端接口

### 支付渠道配置

- `GET /api/payment-channel/list-all`
- `GET /api/payment-channel/list`
- `GET /api/payment-channel/{id}`
- `POST /api/payment-channel`
- `PUT /api/payment-channel/{id}`
- `PATCH /api/payment-channel/{id}/status`
- `DELETE /api/payment-channel/{id}`

### 支付应用配置

- `GET /api/payment-app/list-all`
- `GET /api/payment-app/list`
- `GET /api/payment-app/list-by-channel/{channelId}`
- `GET /api/payment-app/{id}`
- `POST /api/payment-app`
- `PUT /api/payment-app/{id}`
- `PATCH /api/payment-app/{id}/status`
- `DELETE /api/payment-app/{id}`

### 支付配置缓存

- `POST /api/payment-config/reload`
- `GET /api/payment-config/apps`

## 4. 支付流程改造点

- 支付页从 `/api/payment-app/list` 加载启用支付应用，不再硬编码固定按钮。
- 微信 Native V3、微信 Native V2、支付宝电脑网站支付支持传入 `paymentAppId`。
- 订单创建时记录 `payment_app_id` 与 `payment_channel_code`。
- 订单复用逻辑按 `productId + paymentType + paymentAppId + NOTPAY` 区分，避免不同商户应用复用同一未支付订单。
- 微信 V3 HTTP Client 支持按支付应用动态构建真实签名客户端。
- 支付宝支付、查单、关单、退款按订单绑定的支付应用动态构造 `AlipayClient`。
- 微信退款、退款查询、订单退款同步按订单绑定的支付应用选择配置。
- 微信/支付宝异步通知校验按订单绑定配置校验金额、商户号、appid/卖家ID。

## 5. 前端入口

新增页面：`payment-demo-vue/src/views/PaymentConfig.vue`

导航入口：页面顶部“支付配置”。

能力：

- 支付渠道新增、编辑、启用/禁用、删除。
- 支付应用新增、编辑、启用/禁用、删除。
- JSON 配置格式前端校验。
- 手动重新加载后端支付配置缓存。

## 6. 配置 JSON 示例

### 微信渠道公共配置

```json
{
  "domain": "https://api.mch.weixin.qq.com",
  "notifyUrl": "https://your-domain.com"
}
```

### 微信支付应用配置

```json
{
  "appid": "真实微信appid",
  "mchId": "真实微信商户号",
  "mchSerialNo": "真实商户API证书序列号",
  "privateKeyPath": "apiclient_key.pem",
  "apiV3Key": "真实APIv3密钥",
  "partnerKey": "真实APIv2密钥",
  "notifyUrl": "https://your-domain.com"
}
```

### 支付宝渠道公共配置

```json
{
  "gatewayUrl": "https://openapi-sandbox.dl.alipaydev.com/gateway.do",
  "contentKey": "真实内容密钥"
}
```

### 支付宝支付应用配置

```json
{
  "appId": "真实支付宝appId",
  "sellerId": "真实sellerId",
  "merchantPrivateKey": "真实商户私钥",
  "alipayPublicKey": "真实支付宝公钥",
  "returnUrl": "http://localhost:8083/#/success",
  "notifyUrl": "https://your-domain.com/api/ali-pay/trade/notify"
}
```

## 7. 本地验证建议

后端：

```bash
cd payment-demo-v4
mvn clean package
```

前端：

```bash
cd payment-demo-vue
npm install
npm run build
```

数据库：先执行 `payment-demo-v4/sql/payment_demo_v1.sql`，确保 MySQL 8.0 可用。

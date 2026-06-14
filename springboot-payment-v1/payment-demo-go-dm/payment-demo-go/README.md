# payment-demo-go

这是 Payment Demo 的 Go 后端版本，接口路径、统一响应结构、数据库表、Redis 幂等/分布式锁、RabbitMQ 延迟关单、微信 V3/V2、支付宝下单/查单/退款/通知逻辑按原项目对齐。

## 技术栈

- Gin：HTTP 接口
- GORM：达梦数据库 DM8 持久化与事务、`select ... for update`
- go-redis：Redis 幂等与分布式锁
- amqp091-go：RabbitMQ TTL 死信延迟关单和退款自动查询
- 原生 HTTP + RSA/AES：微信支付 V3、微信支付 V2、支付宝 OpenAPI 调用

## 配置来源

默认读取：

- `config/application.yml`
- `config/wxpay.properties`
- `config/alipay-sandbox.properties`
- `config/apiclient_key.pem`

也可以通过环境变量覆盖：

```bash
export APP_CONFIG=config/application.yml
export WXPAY_CONFIG=config/wxpay.properties
export ALIPAY_CONFIG=config/alipay-sandbox.properties
```

`application.yml` 默认使用达梦数据源：

```yaml
spring:
  datasource:
    driver-class-name: dm.jdbc.driver.DmDriver
    url: jdbc:dm://127.0.0.1:5236?schema=PAYMENT_DEMO&connectTimeout=30000
    username: SYSDBA
    password: SYSDBA
```

Go 运行时会把 `jdbc:dm://...` 转换为达梦 Go driver 使用的 `dm://username:password@host:port?...` DSN。
`username` / `password` 会按达梦 Go driver 的解析规则原样拼入 DSN，密码包含 `#`、`@` 等字符时不需要在 `application.yml` 中手动转义。

## 启动

先执行初始化脚本。脚本会创建并切换到 `PAYMENT_DEMO` schema，然后重建表和种子数据：

```bash
disql SYSDBA/SYSDBA@127.0.0.1:5236
SQL> start sql/payment_demo.sql
```

然后启动服务：

```bash
go mod tidy
go run ./cmd/server
```

## 对齐说明

- 统一响应：`{"code":0,"message":"成功","data":{...}}`，失败为 `code=-1`。
- 新下单接口必须提供 `paymentAppId`，订单会绑定 `t_payment_app.id`。
- 支付应用配置合并顺序为 properties 默认值、渠道配置、应用配置。
- 前端首页只选择支付应用，支付方式由应用的 `paymentType` / `channelCode` 推导。
- 订单状态、支付类型、退款状态枚举值保持原中文/英文值。
- 退款申请金额判断保持一致：未失败、未关闭的待审核与已审核退款都会占用可退金额。
- RabbitMQ 延迟关单队列名称、交换机、路由键保持原项目一致；退款审核通过后会投递延迟查询消息，最多自动查询 5 次。
- 微信 V3 通知验签通过 `/v3/certificates` 自动拉取平台证书。

## 主要接口

- `/api/product/test`、`/api/product/list`
- `/api/order-info/list`、`/api/order-info/query-order-status/{orderNo}`
- `/api/wx-pay/native/{productId}?paymentAppId=...`、`/api/wx-pay/native/notify`、`/api/wx-pay/cancel/{orderNo}`、`/api/wx-pay/query/{orderNo}`、`/api/wx-pay/check-order-status/{orderNo}`
- `/api/wx-pay-v2/native/{productId}?paymentAppId=...`、`/api/wx-pay-v2/native/notify`
- `/api/ali-pay/trade/page/pay/{productId}?paymentAppId=...`、`/api/ali-pay/trade/notify`、`/api/ali-pay/trade/close/{orderNo}`、`/api/ali-pay/trade/query/{orderNo}`
- `/api/refund-info/apply`、`/api/refund-info/list`、`/api/refund-info/list/{orderNo}`、`/api/refund-info/approve/{refundNo}`、`/api/refund-info/reject/{refundNo}`、`/api/refund-info/query/{refundNo}`、`/api/refund-info/reconcile/{orderNo}`

## 注意

当前代码没有使用 mock 支付。微信、支付宝接口会按 `config/` 和数据库支付应用配置请求真实/沙箱支付网关。

基础验证命令：

```bash
go test ./...
```

# payment-demo-go

基于上传的 `payment-demo-v4` Java/Spring Boot 项目迁移的 Go 版本，接口路径、统一响应结构、数据库表、Redis 幂等/分布式锁、RabbitMQ 延迟关单、微信 V3/V2、支付宝下单/查单/退款/通知逻辑按原项目对齐。

## 技术栈

- Gin：HTTP 接口
- GORM：MySQL 持久化与事务、`select ... for update`
- go-redis：Redis 幂等与分布式锁
- amqp091-go：RabbitMQ TTL 死信延迟关单
- 原生 HTTP + RSA/AES：微信支付 V3、微信支付 V2、支付宝 OpenAPI 调用

## 配置来源

配置文件直接从原项目复制到 `config/`：

- `config/application.yml`
- `config/wxpay.properties`
- `config/alipay-sandbox.properties`
- `config/apiclient_key.pem`

默认读取上述路径；也可以通过环境变量覆盖：

```bash
export APP_CONFIG=config/application.yml
export WXPAY_CONFIG=config/wxpay.properties
export ALIPAY_CONFIG=config/alipay-sandbox.properties
```

## 启动

```bash
mysql -uroot -p < sql/payment_demo.sql
go mod tidy
go run ./cmd/server
```

## 对齐说明

- 统一响应：`{"code":0,"message":"成功","data":{...}}`，失败为 `code=-1`
- 订单状态、支付类型、退款状态枚举值保持原 Java 项目中文/英文值
- 退款申请金额判断保持一致：未失败/未关闭的待审核与已审核退款都会占用可退金额
- RabbitMQ 延迟关单队列名称、交换机、路由键保持原项目一致
- 微信 V3 通知验签通过 `/v3/certificates` 自动拉取平台证书，使用原项目已有商户配置

## 主要接口保持

- `/api/product/test`、`/api/product/list`
- `/api/order-info/list`、`/api/order-info/query-order-status/{orderNo}`
- `/api/wx-pay/native/{productId}`、`/api/wx-pay/native/notify`、`/api/wx-pay/cancel/{orderNo}`、`/api/wx-pay/query/{orderNo}`、`/api/wx-pay/check-order-status/{orderNo}`
- `/api/wx-pay-v2/native/{productId}`、`/api/wx-pay-v2/native/notify`
- `/api/ali-pay/trade/page/pay/{productId}`、`/api/ali-pay/trade/notify`、`/api/ali-pay/trade/close/{orderNo}`、`/api/ali-pay/trade/query/{orderNo}`
- `/api/refund-info/apply`、`/api/refund-info/list`、`/api/refund-info/list/{orderNo}`、`/api/refund-info/approve/{refundNo}`、`/api/refund-info/reject/{refundNo}`、`/api/refund-info/query/{refundNo}`、`/api/refund-info/reconcile/{orderNo}`

## 注意

当前代码没有使用 mock 支付。微信、支付宝接口会按 `config/` 中原项目配置请求真实/沙箱支付网关。

我在生成包内已完成 `gofmt` 和 Go 语法解析检查；当前沙箱无法访问 `proxy.golang.org`，因此没有生成 `go.sum`。本地第一次启动前执行：

```bash
go mod tidy
```

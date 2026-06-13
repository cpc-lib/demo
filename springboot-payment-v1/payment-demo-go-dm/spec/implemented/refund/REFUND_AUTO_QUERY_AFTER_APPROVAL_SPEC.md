# 退款审核后自动查询 SPEC

## 0. 元信息

- **状态**：implemented
- **日期**：2026-06-13
- **领域**：refund | backend | RabbitMQ | state machine
- **变更类型**：设计变更、MQ 契约补充、退款状态机补链
- **目标**：退款申请审核通过并提交支付渠道后，系统自动通过 RabbitMQ 延迟消息查询渠道退款结果，避免只依赖第三方退款通知或人工查询。

## 1. 背景

当前退款链路已经具备：

- 人工查询接口：`POST /api/refund-info/query/:refundNo`
- 微信 V3 退款通知入口：`POST /api/wx-pay/refunds/notify`
- 支付宝退款查询能力：`alipay.trade.fastpay.refund.query`
- RabbitMQ TTL 死信延迟关单能力

缺口是：审核通过并提交渠道退款后，若第三方退款通知未抵达或不可用，本地退款单可能长期停留在 `PROCESSING`，订单退款汇总状态也不会自动刷新。

## 2. 目标行为

### 2.1 投递时机

- `Approve(ctx, refundNo, remark)` 审核通过、渠道退款提交成功后，投递一条退款查询延迟消息。
- 如果渠道退款提交失败并已标记 `FAILED`，不投递自动查询消息。
- 如果退款提交后本地状态已经是终态，也允许投递；消费者会按终态跳过，保持幂等。

### 2.2 MQ 契约

新增 RabbitMQ 契约：

| 名称 | 值 | 用途 |
|---|---|---|
| event exchange | `payment.refund.query.event.exchange` | 退款查询延迟消息生产入口 |
| dead-letter exchange | `payment.refund.query.dead-letter.exchange` | TTL 到期后的死信交换机 |
| delay queue | `payment.refund.query.delay.queue` | 延迟等待队列 |
| release queue | `payment.refund.query.release.queue` | 消费者查询队列 |
| delay routing key | `payment.refund.query.delay` | 投递到延迟队列 |
| release routing key | `payment.refund.query.release` | 死信投递到消费队列 |

消息 JSON：

```json
{"refundNo":"R202606130001","attempt":1}
```

### 2.3 消费行为

- 消费者收到消息后按 `refundNo` 调用现有统一查询链路 `QueryRefundStatus(ctx, refundNo)`。
- 查询链路继续按订单支付类型分发：
  - 微信：`WxQueryRefundStatusForSync`
  - 支付宝：`AliQueryRefundStatusForSync`
- 消费前若退款单不存在、未审核通过或已处于终态，直接 ack。
- 查询后若仍是 `CREATED` 或 `PROCESSING`，且 `attempt < maxAttempts`，重新投递下一次延迟查询。
- 查询失败时也按剩余次数重新投递下一次延迟查询，避免 RabbitMQ immediate requeue 形成热循环。
- 超过最大次数后不再自动重试，保留人工 `POST /api/refund-info/query/:refundNo` 和对账接口兜底。

### 2.4 终态与重试边界

- 终态：`SUCCESS`、`FAILED`、`CLOSED`、`ABNORMAL`
- 非终态：`CREATED`、`PROCESSING`
- 最大自动查询次数：5 次
- 默认每次延迟：60 秒

## 3. 非目标

- 不新增 HTTP API。
- 不新增 DB 字段或表。
- 不改变现有退款状态枚举值。
- 不改变第三方回调响应结构。
- 不改变人工查询和订单退款对账接口。
- 不保证所有渠道都一定异步退款；支付宝若提交时已同步成功，自动查询消费者会幂等跳过。

## 4. 兼容影响

- **MQ 兼容**：新增一组 RabbitMQ exchange/queue/routing key；不修改现有关单 MQ 名称。
- **HTTP 兼容**：无路径、请求、响应结构变化。
- **DB 兼容**：无 schema 变化；`t_refund_info.refund_status` 和订单退款汇总逻辑复用现有行为。
- **运行兼容**：RabbitMQ 仍为启动必需依赖。退款提交成功后若自动查询消息投递失败，审核接口应返回错误，避免形成无兜底的处理中退款。

## 5. 验收标准

- [x] MQ 特征测试锁住新增 exchange/queue/routing key 名称和退款查询消息 JSON。
- [x] 服务测试或特征测试证明 `PROCESSING` 退款会继续安排自动查询，终态退款不会继续重试。
- [x] 启动时同时启动关单消费者和退款查询消费者。
- [x] 审核通过且渠道退款提交成功后投递退款查询消息。
- [x] `cd payment-demo-go && go test ./...` 通过。

## 6. 实现锚点

- `payment-demo-go/internal/mq/order_close.go`
- `payment-demo-go/internal/mq/refund_query.go`
- `payment-demo-go/internal/model/models.go`
- `payment-demo-go/internal/service/refund_application.go`
- `payment-demo-go/internal/service/refund_auto_query.go`
- `payment-demo-go/cmd/server/main.go`
- `payment-demo-go/internal/mq/order_close_characterization_test.go`
- `payment-demo-go/internal/service/refund_characterization_test.go`

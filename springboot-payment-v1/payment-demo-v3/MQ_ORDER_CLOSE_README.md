# RabbitMQ 延迟关单说明

## 本次改造
- 移除“每 30 秒扫库查未支付订单”的关单方式
- 改为“创建订单后发送 RabbitMQ 延迟消息，到期后只检查这一笔订单”
- 保留退款结果轮询任务，未改动退款对账逻辑

## 处理流程
1. 创建订单成功
2. 发送一条延迟关单消息到 `payment.order.close.delay.queue`
3. 消息 TTL 到期后转发到 `payment.order.close.release.queue`
4. 消费者收到消息后：
   - 如果本地订单已支付/已取消/已关闭，直接跳过
   - 如果仍为未支付，则调用微信/支付宝查单接口核实状态
   - 仍未支付则执行关单

## 默认配置
- RabbitMQ: `192.168.220.200:5672`
- 用户名: `guest`
- 密码: `guest`
- 订单关闭延迟: `60000ms`

## 可调整配置
```yaml
payment:
  order:
    close-delay-ms: 60000
```

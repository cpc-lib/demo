# points-dlq-full-fixed

RabbitMQ + DLX + MySQL + MyBatis-Plus 示例工程

- 注册用户 -> 发送 MQ 消息
- 积分服务消费消息，失败自动重试 3 次
- 超过 3 次进入死信队列 DLX
- 死信队列监听写入人工处理表 manual_process

为避免你现有环境里的 `user.register.q` 等队列参数冲突，本工程使用 **v2 版本队列名称**：

- 业务交换机: `user.register.v2.ex`
- 业务队列: `user.register.v2.q`
- 业务路由键: `user.register.v2`
- 死信交换机: `user.register.v2.dlx.ex`
- 死信队列: `user.register.v2.dlx.q`
- 死信路由键: `user.register.v2.dlx`

只要 RabbitMQ 正常，MySQL 信息与 `application.yml` 保持一致即可直接运行。

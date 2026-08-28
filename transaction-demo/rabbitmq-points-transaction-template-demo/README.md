# RabbitMQ 注册送积分 Demo

## 1. 准备环境

1. 启动 MySQL，修改 `application.yml` 中的连接信息。
2. 执行 `src/main/resources/schema.sql` 初始化数据库。
3. 启动 RabbitMQ（用户名密码参考 `application.yml`）。

## 2. 启动项目

```bash
mvn spring-boot:run
```

## 3. 测试流程

### 3.1 注册

```bash
curl -X POST "http://localhost:8080/api/register?username=tom&mobile=13800000000"
```

注册成功后：
- `user` 表插入一条记录
- 发送 MQ 消息到 `user.register.queue`
- 消费成功后，`user_points` 表插入一条积分记录
- `msg_idempotent` 表记录本次消息已处理

### 3.2 人工处理重发

当消费失败超过 3 次，会写入 `dead_message` 表，然后 ack，避免消息堆积。

人工处理后可通过：

```bash
curl -X POST "http://localhost:8080/api/manual/resend/{id}"
```

重新发送积分消息。

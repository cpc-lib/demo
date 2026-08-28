# 用户注册送积分 Demo（基于 MQ 可靠消息 + 幂等）

## 1. 准备环境

- JDK 8+
- MySQL 5.7+
- RabbitMQ（本地：localhost:5672，guest/guest）

## 2. 初始化数据库

```sql
SOURCE src/main/resources/schema.sql;
```

或者手动执行里面的 SQL。

## 3. 启动项目

```bash
mvn spring-boot:run
```

## 4. 测试流程

1. 调用注册接口：

```bash
curl -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"tom"}'
```

2. 查看数据库：

- `user` 表：新增用户
- `points_log` 表：插入一条 `status=0` 的积分日志

3. 等待 5 秒左右（定时任务扫描）

- `points_log` 的 `status` 会变为 `1`（已发送），随后在消费成功后变为 `2`（已消费）
- `user_points` 表中对应用户的积分增加 100

整个过程对应时序图：

1. 用户服务：新增用户
2. 用户服务：写积分消息日志
3. 定时任务：扫描未发送日志
4. MQ：发送积分增加消息
5. 积分服务：消费消息，增加用户积分（幂等）
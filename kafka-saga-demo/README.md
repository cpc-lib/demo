# Kafka Saga 注册送积分 Demo (Outbox + 补偿)

## 模块说明

- `common-api`：事件 DTO
- `user-service`：用户注册服务，使用 Outbox 表 `t_saga_event_log` 定时发布 `USER_REGISTERED` 事件到 Kafka
- `points-service`：积分服务，消费注册事件发放积分，如果失败则写入补偿事件到本地 Outbox，再由定时任务发布补偿事件到 Kafka，由 `user-service` 消费执行补偿（禁用用户）

## 运行环境

- JDK 17
- Maven 3.8+
- MySQL 5.7+/8.0+
- Kafka 2.x/3.x 及 Zookeeper

## 初始化数据库

建立两个库：

```sql
CREATE DATABASE user_db DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE points_db DEFAULT CHARACTER SET utf8mb4;
```

在两个库中执行 `db_ddl.sql` 中对应表结构：

- `user_db`：`t_user`, `t_saga_event_log`
- `points_db`：`t_points_account`, `t_saga_event_log`

## 编译

```bash
cd kafka-saga-signup-points
mvn clean package -DskipTests
```

## 启动顺序

1. 启动 Kafka/Zookeeper
2. 启动 `user-service`：

```bash
cd user-service
mvn spring-boot:run
```

3. 启动 `points-service`：

```bash
cd points-service
mvn spring-boot:run
```

## 调用示例

### 正常注册（发放积分成功）

```bash
curl -X POST http://localhost:8081/api/user/signup   -H "Content-Type: application/json"   -d '{"username":"tom","phone":"13800000000"}'
```

几秒后（Outbox 定时任务执行）会看到：

- `user_db.t_user` 有新用户
- `points_db.t_points_account` 中有对应用户的 100 积分

### 触发积分失败 + 补偿

在代码中，`PointsService.handleUserRegistered` 中模拟：

> 当用户名包含 `fail` 字符串时抛异常。

所以你可以这样测试：

```bash
curl -X POST http://localhost:8081/api/user/signup   -H "Content-Type: application/json"   -d '{"username":"fail_jack","phone":"13900000000"}'
```

流程：

1. `user-service` 本地事务插入用户 + Outbox 事件 (USER_REGISTERED)
2. Outbox 定时任务发布 Kafka `user.registered.event`
3. `points-service` 收到事件，因用户名包含 `fail` 抛异常
4. `points-service` 捕获异常，写入本地 Outbox (USER_REGISTER_COMPENSATE)
5. `points-service` 的 Outbox 定时任务发布补偿事件 `user.register.compensate`
6. `user-service` 消费补偿事件，执行补偿逻辑：把该用户状态改为 0

你可以在 `user_db.t_user` 中看到该用户 `status = 0`。

## 注意

此示例主要演示 Saga + Outbox + 补偿的完整链路，生产环境需进一步完善：

- 死信队列 / 重试策略
- 分布式追踪链路 ID
- 更细粒度的状态机 / 补偿策略
- 安全、鉴权、监控等

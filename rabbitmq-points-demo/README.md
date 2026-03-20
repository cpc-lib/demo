# RabbitMQ + 定时器 注册送积分 Demo

## 功能说明

- 用户注册成功后，通过 RabbitMQ 发送注册事件。
- 积分服务消费消息，给用户赠送注册积分（默认 100 分）。
- 使用 `points_log` 表做幂等 & 失败重试记录。
- 定时任务每分钟扫描失败记录进行重试。
- 重试超过 5 次，写入 `points_manual_process` 表，后台人工点击再次补发积分。

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 5.7/8.0
- RabbitMQ 3.x（默认 guest/guest, 端口 5672）

## 使用步骤

1. **创建数据库表**

   ```sql
   -- 参考 src/main/resources/schema.sql
   ```

2. **修改数据库、RabbitMQ 配置**

   修改 `src/main/resources/application.yml` 中：

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://127.0.0.1:3306/points_demo?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
       username: root
       password: 123456
     rabbitmq:
       host: 127.0.0.1
       port: 5672
       username: guest
       password: guest
   ```

3. **启动项目**

   ```bash
   mvn spring-boot:run
   ```

4. **调用注册接口**

   ```bash
   curl -X POST http://localhost:8080/user/register      -H "Content-Type: application/json"      -d '{"username":"test","mobile":"13800000000"}'
   ```

   成功后会：

   - 插入 `user_info` 一条记录
   - 通过 RabbitMQ 发送注册消息
   - 积分服务消费后：给用户送 100 积分，写 `points_log` + `user_points`

5. **查看人工处理列表**

   ```bash
   curl http://localhost:8080/manualPoints/pending
   ```

6. **人工点击补偿**

   ```bash
   curl -X POST http://localhost:8080/manualPoints/process/reg_1
   ```

   其中 `reg_1` 是业务 ID（示例：`reg_用户ID`）。

## 说明

- 幂等依赖 `points_log.business_id` 唯一索引。
- MQ 消费、定时任务、人工点击都统一走 `PointsServiceApi#processRegisterEvent`，不会重复送积分。

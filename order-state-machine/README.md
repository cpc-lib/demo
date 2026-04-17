# order-state-machine

一个基于 **Spring Boot 3 + MyBatis-Plus + MySQL** 的数据库驱动订单状态机完整示例。

## 功能点

- 订单状态流转规则存数据库
- Guard 前置校验
- Action 后置动作
- 退款驳回回退到原状态
- 流转日志记录
- MyBatis-Plus 乐观锁
- Caffeine 本地缓存流转规则

## 运行环境

- JDK 17
- Maven 3.8+
- MySQL 8.x

## 初始化数据库

按顺序执行：

1. `src/main/resources/sql/schema.sql`
2. `src/main/resources/sql/data.sql`

## 修改数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/order_machine?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

## 启动项目

```bash
mvn clean package
mvn spring-boot:run
```

## 接口示例

### 创建订单

```bash
curl --location 'http://127.0.0.1:8080/api/orders' --header 'Content-Type: application/json' --data '{
  "orderNo": "ORD202604170003",
  "businessType": "NORMAL",
  "amount": 299.00,
  "remark": "新建订单"
}'
```

### 查询订单列表

```bash
curl --location 'http://127.0.0.1:8080/api/orders'
```

### 通用状态流转

```bash
curl --location 'http://127.0.0.1:8080/api/orders/transit' --header 'Content-Type: application/json' --data '{
  "orderId": 1,
  "event": "PAY",
  "operator": "admin",
  "remark": "订单支付"
}'
```

### 快捷接口

支付：

```bash
curl --location --request POST 'http://127.0.0.1:8080/api/orders/1/pay?operator=admin'
```

发货：

```bash
curl --location --request POST 'http://127.0.0.1:8080/api/orders/1/ship?operator=admin'
```

确认收货：

```bash
curl --location --request POST 'http://127.0.0.1:8080/api/orders/1/confirm-receipt?operator=admin'
```

申请退款：

```bash
curl --location --request POST 'http://127.0.0.1:8080/api/orders/2/apply-refund?operator=admin'
```

退款驳回：

```bash
curl --location --request POST 'http://127.0.0.1:8080/api/orders/2/refund-reject?operator=admin'
```

退款成功：

```bash
curl --location --request POST 'http://127.0.0.1:8080/api/orders/2/refund-success?operator=admin'
```

## 设计说明

- `t_order_state_transition`：定义允许的流转规则
- `OrderStateMachineService`：状态机入口
- `StateGuard`：前置校验
- `StateAction`：后置动作
- `t_order_state_log`：流转日志

## 建议的生产优化

- 把规则缓存升级为 Redis + 本地缓存双层缓存
- 给流转规则做后台配置和发布审核
- 使用条件更新 SQL 增强并发控制
- 接入 MQ 处理支付、发货、完结等后续动作

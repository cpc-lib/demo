# clickhouse-springboot-demo

一个**完整可运行**的 Spring Boot + ClickHouse Demo，包含：

- docker-compose 一键启动 ClickHouse（含初始化建库/建表/初始化数据）
- Spring Boot 通过 JDBC 访问 ClickHouse
- REST API：ping / 列表 / 新增 / 统计
- JUnit 集成测试：验证 ClickHouse 连通与查询

## 环境要求

- JDK 17+
- Maven 3.8+
- Docker + docker-compose

## 1. 启动 ClickHouse

在项目根目录执行：

```bash
docker-compose up -d
```

检查是否启动完成：

```bash
curl "http://localhost:8123/ping"
curl -u demo:demo123 "http://localhost:8123/?database=demo&query=SELECT%20count()%20FROM%20t_order"
```

## 2. 启动 Spring Boot

```bash
mvn -q -DskipTests=false test
mvn spring-boot:run
```

## 3. API 测试

### 3.1 Ping

```bash
curl http://localhost:8080/api/ch/ping
```

### 3.2 查询订单总数

```bash
curl http://localhost:8080/api/orders/count
```

### 3.3 查询订单列表

```bash
curl "http://localhost:8080/api/orders?limit=10"
```

### 3.4 新增订单

```bash
curl -v -X POST "http://localhost:8080/api/orders" \
  -H "Content-Type: application/json" \
  -H "Accept: text/plain" \
  -d '{"id":101,"userId":2001,"amount":88.66,"status":"PAID"}'
```

## 4. 常见问题

- 端口：8123 是 HTTP/JDBC 常用端口；9000 是 Native。
- 初始化 SQL：放在 `clickhouse/init/` 并挂载到 `/docker-entrypoint-initdb.d`。
- ClickHouse 启动较慢：先等待 healthcheck 通过后再启动应用/跑测试。


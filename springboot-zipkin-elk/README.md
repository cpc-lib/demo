# Micrometer + Zipkin + ELK 调用链路追踪 Demo v2

本版本将 `order-service` 和 `user-service` 做成两个**完全独立的 Spring Boot 工程**，互不依赖父子模块，结构更简单，保证可以单独编译运行。

## 一、项目结构

- `order-service/`：订单服务，端口 `8081`，对外暴露 `/orders/{id}`，内部通过 RestTemplate 调用 user-service
- `user-service/`：用户服务，端口 `8082`，对外暴露 `/users/{id}`
- `docker/`：Zipkin + Elasticsearch + Logstash + Kibana
- `postman/`：Postman 调用集合

两个服务都已集成：

- Micrometer Tracing + Zipkin (Brave bridge)
- logback + logstash-logback-encoder（输出 traceId / spanId，并通过 TCP 发给 Logstash）
- actuator + tracing 配置

## 二、启动步骤

### 1. 启动 Zipkin + ELK

```bash
cd docker
docker compose up -d
```

服务端口：

- Zipkin: http://localhost:9411
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601
- Logstash TCP: 5044

### 2. 编译并启动 `user-service`

```bash
cd user-service
mvn clean package -DskipTests
mvn spring-boot:run
```

### 3. 编译并启动 `order-service`

```bash
cd ../order-service
mvn clean package -DskipTests
mvn spring-boot:run
```

### 4. 使用 Postman 触发调用

导入：`postman/Micrometer-Zipkin-ELK-Demo-v2.postman_collection.json`

- 请求 1：`查询订单（触发链路追踪）`
  - GET `http://localhost:8081/orders/1`
  - order-service 会内部调用 `http://localhost:8082/users/1`
  - Micrometer 会自动生成 traceId / spanId，并在两个服务之间透传
- 请求 2：`直接查询用户`
  - GET `http://localhost:8082/users/1`

### 5. 在 Zipkin 中查看调用链

打开：`http://localhost:9411`

- Service Name 里可以看到 `order-service` 和 `user-service`
- 选择某个服务，点击 `Find Traces`
- 可以看到一次请求的完整调用链（order-service -> user-service）

### 6. 在 Kibana 中查看日志

首次访问 Kibana：`http://localhost:5601`

1. 创建 Index Pattern：`spring-logs-*`
2. 在 Discover 中选择该 index
3. 搜索 `traceId : "xxx"` 即可查看同一次调用的所有日志

日志格式示例（控制台）：

```text
2025-11-26 10:00:00.000 INFO  [order-service,traceId=...,spanId=...] ...
```

## 三、注意事项

- 如需修改 Zipkin 地址，只需修改两个服务的 `application.yml` 中：

```yaml
management:
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

- 如在你本地 Logstash 不在 5044 端口，可以调整两个服务的 `logback-spring.xml`：

```xml
<property name="LOGSTASH_HOST" value="${LOGSTASH_HOST:-localhost}"/>
<property name="LOGSTASH_PORT" value="${LOGSTASH_PORT:-5044}"/>
```

然后以环境变量方式覆盖：

```bash
LOGSTASH_HOST=your-host LOGSTASH_PORT=your-port mvn spring-boot:run
```


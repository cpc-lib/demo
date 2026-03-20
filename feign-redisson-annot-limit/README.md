# Feign + Redisson 注解限流 Demo

## 功能

- 使用 `@ConcurrencyLimit` 注解，基于 Redisson 分布式信号量控制并发
- 对 Feign 调用外部 API 进行全局并发限制（多实例共享）
- 演示非阻塞限流和支持排队等待模式
- 集成 Spring Cloud OpenFeign + LoadBalancer（2023.0.1）

## 运行步骤

1. 启动 Redis（使用 docker-compose）

```bash
docker-compose up -d
```

2. 编译并启动应用

```bash
mvn clean package
mvn spring-boot:run
```

3. 测试接口

- 非阻塞限流（外部系统并发最多 5）

```bash
curl "http://localhost:8080/api/external"
```

并发压测时，超过并发会返回 HTTP 429 和消息：

```text
外部系统并发数已满，请稍后再试
```

- 支持排队等待模式（本地慢业务）

```bash
curl "http://localhost:8080/api/slow"
```

并发较高时，多余请求会排队，超时 5s 仍未拿到信号量则返回：

```text
本地排队超时，请稍后再试
```

## 核心注解使用方式

```java
@ConcurrencyLimit(
    value = 5,
    key = "external-api",
    blocking = false,
    message = "外部系统并发数已满，请稍后再试"
)
public String callExternal() { ... }
```

你可以将此注解加在任何 Service 方法上，实现按业务维度的分布式并发控制。

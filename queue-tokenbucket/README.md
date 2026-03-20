# 支持排队等待 + 自适应令牌桶 的分布式限流 Demo（含详细中文注释）

## 一、功能说明

本 Demo 基于：

- Spring Boot 3.2.5
- Redisson（Redis）分布式组件
- Spring AOP + 注解

实现了一个 **组合限流** 方案：

1. **令牌桶（RRateLimiter）控制总体 QPS**
2. **分布式信号量（RSemaphore）控制最大并发数**
3. **可选排队等待（带超时时间）**

整体流量控制策略：

- 先看“有没有令牌”（整体流量够不够）
- 再看“有没有空位”（并发是不是已经打满）
- 如果允许排队，则在一定时间内等待空位，否则立即拒绝

---

## 二、启动步骤

### 1. 启动 Redis（推荐用 docker-compose）

```bash
docker-compose up -d
```

确认 6379 已经监听：

```bash
docker ps
```

### 2. 启动 Spring Boot

```bash
mvn clean package
mvn spring-boot:run
```

### 3. 访问测试接口

```bash
curl "http://localhost:8080/work"
```

正常情况下，你会看到类似输出：

```text
OK - 1733222222222
```

---

## 三、压测验证限流效果

1. 单接口：`GET /work`
2. 每个请求会 sleep 500ms 模拟业务耗时
3. 注解参数（在 `TestController#doWork` 上）：

```java
@QueueTokenLimit(
    key = "demo:work",
    maxConcurrency = 3,
    enableQueue = true,
    queueTimeoutMs = 2000,
    tokenRate = 10,
    tokenBucketSize = 20,
    message = "系统繁忙，请稍后重试"
)
```

含义：

- 同时只有 3 个请求在执行业务逻辑（maxConcurrency=3）
- 允许排队等待，最多等 2 秒（超过则直接失败）
- 整体 QPS ≈ 10（令牌桶控制）
- 当流量过大时，多余请求会返回 HTTP 429：

```text
系统繁忙，请稍后重试
```

或者更具体的提示：

- `自适应令牌桶拒绝请求：...`
- `排队等待超时：...`
- `并发已满：...`

---

## 四、如何集成到你自己的项目中？

1. 拷贝以下类到你的项目中：

- `QueueTokenLimit`（注解）
- `QueueTokenLimitAspect`（限流切面）
- `LimitException`（异常）
- `GlobalHandler`（全局异常处理）

2. 在你的启动类上加：

```java
@EnableAspectJAutoProxy(proxyTargetClass = true)
```

3. 配置 Redisson：

在 `application.yml` 中确认：

```yaml
redisson:
  config: |
    singleServerConfig:
      address: "redis://你的Redis地址:6379"
```

4. 在目标接口 / 方法上添加注解：

```java
@QueueTokenLimit(
    key = "order:create",
    maxConcurrency = 20,
    enableQueue = true,
    queueTimeoutMs = 3000,
    tokenRate = 100,
    tokenBucketSize = 200,
    message = "下单过于频繁，请稍后再试"
)
public void createOrder(...) { ... }
```

即可实现“按业务维度”的分布式限流。

---

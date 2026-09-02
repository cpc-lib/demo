# SHA-256 Distributed Async Demo

一个可以直接放进 IDEA 的 Java 17 / Spring Boot 多模块示例：**前端上传文件，API 立即创建异步任务，Redis 保存任务状态，RabbitMQ 或 Kafka 负责解耦，独立 Worker 流式计算 SHA-256。**

## 1. 架构

```text
Browser
  │ multipart upload
  ▼
sha256-api :8080
  ├── 文件落到共享 storage
  ├── Redis: sha256:task:{taskId} = QUEUED
  └── RabbitMQ / Kafka 发布 Sha256TaskMessage
                     │
                     ▼
              sha256-worker × N
                ├── Redis 分布式 task lock
                ├── 流式读取文件
                ├── 每 8MB 更新一次 Redis 进度
                ├── 保存 SUCCESS / FAILED
                └── 删除临时上传文件

Browser -- GET /api/sha256/tasks/{taskId} --> Redis task state
```

**API 与 Worker 不在同一个 JVM 中。** Worker 停掉时，API 仍然可以接收请求并把任务放到队列；Worker 恢复后继续消费。

## 2. 模块

```text
sha256-distributed-demo/
├── sha256-common/     # Task DTO + Redis Repository
├── sha256-api/        # WebFlux 上传接口 + 前端 + MQ Producer
├── sha256-worker/     # RabbitMQ/Kafka Consumer + SHA-256 Worker
├── docker-compose.yml # Redis + RabbitMQ + API + Worker
└── docker-compose.kafka.yml # Redis + Kafka + API + Worker
```

## 3. 最简单：RabbitMQ 一键启动

项目根目录：

```bash
docker compose up --build
```

浏览器：

```text
http://localhost:8080
```

RabbitMQ 管理端：

```text
http://localhost:15672
username: sha256
password: sha256-demo
```

## 4. Kafka 模式

不要同时启动 RabbitMQ 版本和 Kafka 版本。

```bash
docker compose -f docker-compose.kafka.yml up --build
```

浏览器仍然访问：

```text
http://localhost:8080
```

页面顶部会显示 `KAFKA`。

## 5. IDEA 本地运行（推荐调试方式）

### 5.1 RabbitMQ

先运行：

```text
scripts/start-infra-rabbitmq.cmd
```

给 **API 和 Worker 两个 Run Configuration** 都添加环境变量：

```text
RABBITMQ_USERNAME=sha256
RABBITMQ_PASSWORD=sha256-demo
```

然后 IDEA 分别运行：

```text
sha256-api/src/main/java/com/example/sha256/api/Sha256ApiApplication.java
sha256-worker/src/main/java/com/example/sha256/worker/Sha256WorkerApplication.java
```

两边默认都使用：

```text
${user.home}/.sha256-demo/uploads
```

所以即使 API 和 Worker 是两个 Java 进程，也能访问同一份上传文件。

### 5.2 Kafka

先运行：

```text
scripts/start-infra-kafka.cmd
```

给 **API 和 Worker 两个 Run Configuration** 都添加环境变量：

```text
SHA256_BROKER=kafka
KAFKA_BOOTSTRAP_SERVERS=127.0.0.1:9092
```

然后分别点击 Run。

## 6. HTTP API

### 创建任务

```http
POST /api/sha256/tasks
Content-Type: multipart/form-data
file=<binary>
```

成功：

```json
{
  "taskId": "f3f5...",
  "status": "QUEUED",
  "broker": "rabbitmq"
}
```

HTTP 状态码：`202 Accepted`。

### 查询任务

```http
GET /api/sha256/tasks/{taskId}
```

计算中：

```json
{
  "taskId": "f3f5...",
  "originalFilename": "demo.zip",
  "totalBytes": 104857600,
  "processedBytes": 50331648,
  "progress": 48,
  "status": "RUNNING",
  "sha256": null,
  "error": null,
  "broker": "rabbitmq"
}
```

完成：

```json
{
  "status": "SUCCESS",
  "progress": 100,
  "sha256": "33da92f6..."
}
```

## 7. 为什么不会阻塞 HTTP 线程

1. `sha256-api` 只负责接收上传、写任务状态、发送 MQ 消息。
2. SHA-256 **不在 API 进程计算**。
3. `sha256-worker` 是单独进程，可以部署 N 个实例。
4. 前端通过 taskId 查询进度，不需要让上传请求一直挂着等待 SHA-256。
5. Redis 使用 reactive client；RabbitMQ 发送被调度到 boundedElastic；Kafka Producer 直接桥接异步 Future。

> 注意：文件上传本身当然需要占用一个 HTTP 连接直到上传完成；“异步”指的是上传完成后的 SHA-256 计算与 HTTP 请求生命周期解耦。

## 8. 幂等与可靠性

Worker 消费前使用 Redis：

```text
SET sha256:lock:{taskId} 1 NX EX <ttl>
```

用于避免 RabbitMQ 重投、Kafka 重平衡或多 Worker 同时处理同一任务。

另外：

- `SUCCESS` 任务再次收到消息会直接跳过；
- RabbitMQ Queue / Exchange 都是 durable；
- Kafka Producer 使用 `acks=all`；
- 任务状态带 TTL，默认 30 分钟；
- Worker 完成后删除临时文件；
- 进度按批次写 Redis，避免每读取一个 buffer 就打一次 Redis。

## 9. 横向扩容 Worker

RabbitMQ：

```bash
docker compose up --build --scale worker=4
```

Kafka：`sha256.kafka.partitions` 默认 6，因此同一个 consumer group 最多可以有效并行 6 个消费者线程/实例（超过分区数量的消费者会空闲）。

## 10. 生产环境建议

本 Demo 使用共享磁盘卷，是为了本机一键运行。生产环境建议将上传文件改为：

```text
Browser -> API -> MinIO / S3 / OSS
                    │ storageKey
                    ▼
                MQ message
                    ▼
                  Worker
```

这样 API 和 Worker 可以跨机器、跨 Kubernetes Pod 部署，不依赖共享磁盘。

若要进一步加强可靠性，建议追加：

- RabbitMQ Publisher Confirm + Return 处理 / Outbox
- Kafka 幂等 Producer / 事务性 Outbox
- DLQ / Retry Topic
- Redis Lua 原子状态迁移
- MinIO/S3 对象存储
- Prometheus 指标与任务耗时统计
- 限流、鉴权、配额与最大文件大小

## 11. 编译

```bash
mvn clean test
mvn clean package
```

Java：17+

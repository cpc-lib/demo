# SHA-256 Distributed Async Demo v3

Java 17 + Spring Boot 多模块示例，支持：

- 前端文件上传与计算进度展示
- **MinIO / AWS S3 / 阿里云 OSS（S3 API）对象存储 + `storageKey`**
- Redis + Lua 原子任务状态机
- MySQL Transactional Outbox
- RabbitMQ Retry Queue + DLQ
- Kafka Non-blocking Retry Topic + DLT
- 独立 Worker，可跨机器 / Kubernetes Pod 横向扩容
- SHA-256 流式计算，不把整个文件加载到 JVM Heap

## 1. 最终架构

```text
Browser
   │ multipart upload
   ▼
sha256-api :8080
   │
   ├─ 临时文件仅存在于 API 本机上传期间
   │
   ├─ PutObject
   ▼
MinIO / AWS S3 / Alibaba OSS
   │
   │ storageKey
   ▼
MySQL Transaction
   ├─ sha256_task
   └─ outbox_event(PENDING)
            │
            ▼
      Outbox Dispatcher
            │ publisher confirm / broker ack
            ▼
     RabbitMQ / Kafka
            │
            ▼
       sha256-worker × N
            │
            ├─ Redis Lua claim + lease
            ├─ GetObject(storageKey)
            ├─ stream -> MessageDigest(SHA-256)
            ├─ Redis Lua progress/status
            └─ success -> DeleteObject(optional)

RabbitMQ failure:
main queue -> retry queue(TTL) -> main queue -> ... -> DLQ

Kafka failure:
main topic -> retry topic(s) -> ... -> DLT

Browser -> GET /api/sha256/tasks/{taskId} -> Redis Lua state
```

**API 和 Worker 不共享磁盘，不要求部署在同一台机器。**

## 2. Maven 模块

```text
sha256-distributed-demo/
├── env/
│   ├── docker-compose.yml        # Redis + MySQL + MinIO + RabbitMQ + Kafka，无 build
│   └── mysql/init/schema.sql
├── sha256-common/
│   ├── model/
│   ├── repository/               # Redis Lua 状态机
│   ├── storage/                  # S3-compatible ObjectStorageService
│   └── resources/redis/*.lua
├── sha256-api/
│   ├── controller/
│   ├── service/
│   ├── persistence/              # sha256_task + outbox_event
│   ├── outbox/                   # Outbox Dispatcher
│   ├── broker/
│   └── resources/static/         # 前端页面
└── sha256-worker/
    ├── consumer/
    ├── service/
    └── config/
```

## 3. 启动环境

`env/docker-compose.yml` **只有环境，没有任何 `build:`**。

项目根目录执行：

```bash
docker compose -f env/docker-compose.yml up -d
```

查看：

```bash
docker compose -f env/docker-compose.yml ps
```

停止：

```bash
docker compose -f env/docker-compose.yml down
```

### 默认环境

| 服务 | 地址 | 账号 |
|---|---|---|
| Redis | `192.168.1.200:6379` | - |
| MySQL | `192.168.1.200:3306/sha256_demo` | `sha256 / sha256-demo` |
| MinIO API | `http://192.168.1.200:9000` | `sha256minio / sha256-minio-secret` |
| MinIO Console | `http://192.168.1.200:9001` | 同上 |
| RabbitMQ | `192.168.1.200:5672` | `sha256 / sha256-demo` |
| RabbitMQ UI | `http://192.168.1.200:15672` | `sha256 / sha256-demo` |
| Kafka | `192.168.1.200:9092` | - |

默认对象存储 Bucket：

```text
sha256-files
```

API / Worker 启动时会检查 Bucket；MinIO 本地模式默认允许自动创建。

## 4. IDEA 直接运行

### RabbitMQ 模式（默认）

先启动：

```text
scripts/start-infra-rabbitmq.cmd
```

然后 IDEA 分别运行：

```text
sha256-api/src/main/java/com/example/sha256/api/Sha256ApiApplication.java
sha256-worker/src/main/java/com/example/sha256/worker/Sha256WorkerApplication.java
```

浏览器：

```text
http://localhost:8080
```

默认不需要额外环境变量。

### Kafka 模式

先启动：

```text
scripts/start-infra-kafka.cmd
```

API 与 Worker 的 IDEA Run Configuration 增加：

```text
SHA256_BROKER=kafka
```

然后分别 Run。

## 5. storageKey

上传后生成类似：

```text
sha256/2026/08/30/af268c81a9764ee3935028d388b04713
```

MQ 中不发送文件内容，也不发送本机路径，只发送：

```json
{
  "taskId": "af268c81a9764ee3935028d388b04713",
  "storageKey": "sha256/2026/08/30/af268c81a9764ee3935028d388b04713",
  "storageBucket": "sha256-files",
  "originalFilename": "demo.zip",
  "totalBytes": 104857600,
  "broker": "rabbitmq"
}
```

因此 Worker 只需要对象存储凭证即可读取文件。

## 6. MinIO / S3 / OSS 切换

统一使用 AWS SDK v2 的 S3 API。

### MinIO（默认）

```text
SHA256_STORAGE_PROVIDER=minio
SHA256_STORAGE_ENDPOINT=http://192.168.1.200:9000
SHA256_STORAGE_REGION=us-east-1
SHA256_STORAGE_ACCESS_KEY=sha256minio
SHA256_STORAGE_SECRET_KEY=sha256-minio-secret
SHA256_STORAGE_BUCKET=sha256-files
SHA256_STORAGE_PATH_STYLE=true
SHA256_STORAGE_CHUNKED_ENCODING=true
SHA256_STORAGE_AUTO_CREATE_BUCKET=true
```

### AWS S3

建议：

```text
SHA256_STORAGE_PROVIDER=s3
SHA256_STORAGE_ENDPOINT=
SHA256_STORAGE_REGION=us-east-1
SHA256_STORAGE_ACCESS_KEY=<AWS_ACCESS_KEY>
SHA256_STORAGE_SECRET_KEY=<AWS_SECRET_KEY>
SHA256_STORAGE_BUCKET=<bucket>
SHA256_STORAGE_PATH_STYLE=false
SHA256_STORAGE_CHUNKED_ENCODING=true
SHA256_STORAGE_AUTO_CREATE_BUCKET=false
```

如果不配置 access/secret，代码会使用 AWS Default Credentials Provider Chain。

### 阿里云 OSS S3 API

示例：

```text
SHA256_STORAGE_PROVIDER=oss
SHA256_STORAGE_ENDPOINT=https://s3.oss-cn-hangzhou.aliyuncs.com
SHA256_STORAGE_REGION=cn-hangzhou
SHA256_STORAGE_ACCESS_KEY=<ALIYUN_ACCESS_KEY_ID>
SHA256_STORAGE_SECRET_KEY=<ALIYUN_ACCESS_KEY_SECRET>
SHA256_STORAGE_BUCKET=<bucket>
SHA256_STORAGE_PATH_STYLE=false
SHA256_STORAGE_CHUNKED_ENCODING=false
SHA256_STORAGE_AUTO_CREATE_BUCKET=false
```

OSS 的 S3 协议只支持 virtual-hosted style，因此 `PATH_STYLE=false`。

## 7. Transactional Outbox

上传对象成功后，API 执行一个 MySQL 本地事务：

```text
BEGIN
  INSERT sha256_task
  INSERT outbox_event(status=PENDING)
COMMIT
```

API **不会在这个事务中直接调用 RabbitMQ/Kafka**。

`OutboxDispatcher` 定时扫描：

```sql
SELECT ...
FROM outbox_event
WHERE status IN ('PENDING', 'FAILED')
  AND next_retry_at <= NOW(3)
ORDER BY id
LIMIT ?
FOR UPDATE SKIP LOCKED;
```

然后：

```text
PENDING
  -> SENDING
  -> broker ACK / publisher confirm
  -> SENT
```

发送失败：

```text
SENDING -> FAILED -> exponential backoff -> SENDING
```

这允许多个 API 实例同时跑 Outbox Dispatcher，并避免重复 claim 同一批记录。

## 8. Redis Lua 状态机

Redis 不再使用“GET JSON -> Java 修改 -> SET JSON”的竞争写方式。

每个状态修改通过 Lua 完成：

```text
create_task.lua
claim_task.lua
update_progress.lua
mark_success.lua
mark_retrying.lua
mark_failed.lua
mark_dead_lettered.lua
release_lock.lua
```

状态：

```text
QUEUED
  │
  ▼
RUNNING ───────► SUCCESS
  │
  └──── failure ─► RETRYING ─► RUNNING
                         │
                         └──── retries exhausted ─► DEAD_LETTERED
```

### Worker lease

`claim_task.lua` 原子完成：

```text
检查任务状态
+
SET sha256:lock:{taskId} <workerToken> NX PX <lease>
+
status = RUNNING
```

进度 Lua 每次更新时同时续约 lease。

释放锁时比较 `workerToken` 后再 DEL，防止旧 Worker 删除新 Worker 的锁。

## 9. RabbitMQ Retry + DLQ

默认：

```text
main exchange  : sha256.exchange
main queue     : sha256.tasks

retry exchange : sha256.retry.exchange
retry queue    : sha256.tasks.retry
retry delay    : 5000 ms

DLX            : sha256.dlx
DLQ            : sha256.tasks.dlq
max retries    : 3
```

流程：

```text
sha256.tasks
   │ fail
   ▼
sha256.tasks.retry
   │ TTL=5s
   ▼
sha256.tasks
   │
   ├─ success -> ACK
   │
   └─ retry exhausted -> sha256.tasks.dlq
```

重试次数放在消息 Header：

```text
x-sha256-retry
```

对象在失败 / DLQ 时默认**不会删除**，方便人工修复后重放。

## 10. Kafka Retry Topic + DLT

Kafka 使用 Spring Kafka `@RetryableTopic`：

```text
sha256.tasks
  -> sha256.tasks-retry-...
  -> ...
  -> sha256.tasks-dlt
```

默认：

```text
attempts      = 4
initial delay = 5000 ms
multiplier    = 2.0
max delay     = 60000 ms
```

DLT handler 会把 Redis 状态改为：

```text
DEAD_LETTERED
```

## 11. HTTP API

### 上传并创建任务

```http
POST /api/sha256/tasks
Content-Type: multipart/form-data
file=<binary>
```

返回：

```http
202 Accepted
```

```json
{
  "taskId": "af268c81a9764ee3935028d388b04713",
  "status": "QUEUED",
  "broker": "rabbitmq"
}
```

注意：HTTP 返回 202 前，文件已经成功写入对象存储，并且 MySQL 中的 Task + Outbox 已经提交。
SHA-256 **不会在 HTTP 请求线程中计算**。

### 查询任务

```http
GET /api/sha256/tasks/{taskId}
```

重试中示例：

```json
{
  "status": "RETRYING",
  "retryCount": 2,
  "progress": 48,
  "error": "temporary object storage error"
}
```

成功：

```json
{
  "status": "SUCCESS",
  "progress": 100,
  "sha256": "33da92f6cc1b0478f94a89df645d9e241f213107425a86dff8bb28b3bc6b8d4f"
}
```

## 12. 为什么真正支持跨机器

旧架构：

```text
API -> /shared/uploads/abc
Worker -> /shared/uploads/abc
```

要求 API/Worker 挂载同一份 Volume。

现在：

```text
API -> S3 PutObject(storageKey)
MQ  -> storageKey
Worker -> S3 GetObject(storageKey)
```

因此：

- API 可以在机器 A；
- Worker 可以在机器 B/C/D；
- Kubernetes Pod 不需要 RWX Volume；
- Worker 可以独立扩缩容；
- 文件生命周期由对象存储管理。

## 13. 编译

```bash
mvn clean test
mvn clean package
```

要求：

```text
Java 17+
Maven 3.9+
```

## 14. 本地验证建议

1. `docker compose -f env/docker-compose.yml up -d`
2. 启动 API + Worker。
3. 打开 `http://localhost:8080` 上传文件。
4. MinIO Console 检查对象创建。
5. MySQL 查看：

```sql
SELECT * FROM sha256_task ORDER BY created_at DESC;
SELECT * FROM outbox_event ORDER BY id DESC;
```

6. RabbitMQ 模式下关闭 Worker 后上传文件，再启动 Worker，任务应继续处理。
7. 故意配置错误的对象存储凭证测试 Retry / DLQ。
8. RabbitMQ UI 查看 `sha256.tasks.retry` / `sha256.tasks.dlq`。


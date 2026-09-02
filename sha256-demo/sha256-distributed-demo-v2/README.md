# SHA-256 Distributed Demo v2

大文件在线 SHA-256 计算 Demo，重点解决一次性上传大文件速度慢、HTTP 请求线程长时间占用以及网络中断后必须重新上传的问题。

## v2 核心升级

- 浏览器分片上传，默认 16 MiB/Part。
- 默认 4 路并发，可通过 `SHA256_UPLOAD_CONCURRENCY` 调整。
- 浏览器直接使用 S3 Presigned URL 上传到 MinIO/S3/OSS，文件数据不再经过 API 中转。
- S3/MinIO 原生 Multipart Upload 合并，API 不在本地拼接大文件。
- 断点续传：Redis 保存上传会话，对象存储 `ListParts` 作为真实分片状态，浏览器 localStorage 作为辅助缓存。
- 同一文件刷新页面、断网或重新打开后，重新选择文件即可只上传缺失 Part。
- 每个分片浏览器侧自动重试 3 次，退避间隔逐步增加。
- 合并前服务端校验 Part 数量、顺序和大小，避免残缺对象进入计算链路。
- 合并成功后才创建 `sha256_task + outbox_event`，继续复用原有 Redis Lua + RabbitMQ/Kafka + Worker + Retry/DLQ 架构。

## 上传链路

```text
Browser
  │ 1. POST /api/sha256/uploads/init
  │
  │ 2. 获取缺失 Part 的 Presigned PUT URL
  │
  ├────────── Part 1 ──────────► MinIO / S3 / OSS
  ├────────── Part 2 ──────────► MinIO / S3 / OSS
  ├────────── Part 3 ──────────► MinIO / S3 / OSS
  └────────── Part N ──────────► MinIO / S3 / OSS
                     │
                     │ Multipart Complete
                     ▼
                 完整对象
                     │
                     ▼
        MySQL Task + Transactional Outbox
                     │
                     ▼
            RabbitMQ / Kafka
                     │
                     ▼
                Worker × N
                     │
                     ▼
              Redis Lua 状态机
```

## 断点续传原理

上传初始化时前端根据：

- 文件名
- 文件大小
- lastModified
- 文件头 1 MiB
- 文件尾 1 MiB

生成恢复用 fingerprint。

服务端使用 fingerprint 查询 Redis：

```text
sha256:upload:fingerprint:{fingerprint}
        ↓
sha256:upload:session:{sessionId}
        ↓
uploadId + storageKey + partSize + totalParts
```

随后调用对象存储 `ListParts`。已经存在的 Part 不再上传，因此断点依据不是仅靠浏览器缓存，而是以 MinIO/S3 中实际存在的 Part 为准。

## API

### 初始化 / 恢复

```http
POST /api/sha256/uploads/init
Content-Type: application/json
```

```json
{
  "fileName": "large.zip",
  "fileSize": 10737418240,
  "lastModified": 1788000000000,
  "contentType": "application/zip",
  "fingerprint": "..."
}
```

返回包含 `sessionId`、`partSize`、`totalParts`、`uploadedParts`、`uploadedBytes` 和推荐并发数。

### 查询上传会话

```http
GET /api/sha256/uploads/{sessionId}
```

### 批量获取 Part 预签名 URL

```http
POST /api/sha256/uploads/{sessionId}/presign
```

```json
{"partNumbers":[1,2,3,4]}
```

### 完成合并

```http
POST /api/sha256/uploads/{sessionId}/complete
```

合并成功后创建 SHA-256 异步计算任务并返回 `taskId`。

### 主动取消

```http
DELETE /api/sha256/uploads/{sessionId}
```

会调用 S3 `AbortMultipartUpload`。

## IDEA 本地启动

先启动环境：

```bash
docker compose -f env/docker-compose.yml up -d
```

再分别运行：

```text
sha256-api/src/main/java/com/example/sha256/api/Sha256ApiApplication.java
sha256-worker/src/main/java/com/example/sha256/worker/Sha256WorkerApplication.java
```

浏览器：

```text
http://localhost:8080
```

> `sha256.storage.public-endpoint` 必须是浏览器可以访问的 MinIO/S3 地址。API 内部 endpoint 和浏览器 public endpoint 可以不同。若 MinIO 在虚拟机中，请把它设置为虚拟机 IP，而不是 `localhost`。

## 默认环境

| 服务 | 地址 |
|---|---|
| API | http://localhost:8080 |
| Redis | localhost:6379 |
| MySQL | localhost:3306 |
| MinIO S3 API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 |
| RabbitMQ | localhost:5672 |
| RabbitMQ Console | http://localhost:15672 |
| Kafka | localhost:9092 |

MinIO：

```text
username: sha256minio
password: sha256-minio-secret
bucket: sha256-files
```

RabbitMQ：

```text
username: sha256
password: sha256-demo
```

## 上传参数

`sha256-api/src/main/resources/application.yml`：

```yaml
sha256:
  upload:
    part-size-mb: 16
    recommended-concurrency: 4
    presign-expire-minutes: 30
    session-ttl-hours: 24
    completed-resume-minutes: 10
    max-presign-batch: 100
```

生产环境建议：

- Part Size 16-64 MiB，根据网络和文件大小调整。
- 并发通常 4-8；并不是越大越快，需要避免客户端带宽和对象存储被打满。
- `MINIO_API_CORS_ALLOW_ORIGIN` 不要使用 `*`，限制为实际前端域名。
- 配置对象存储 Lifecycle，自动清理长期未完成的 Multipart Upload。
- Redis 上传会话 TTL 与对象存储未完成 Multipart 生命周期保持一致。

## 说明

SHA-256 的最终计算仍然由 Worker 从合并后的对象流式读取完成，不在浏览器端提前计算整个文件，因此分片并发上传不会改变最终 SHA-256 的计算结果。

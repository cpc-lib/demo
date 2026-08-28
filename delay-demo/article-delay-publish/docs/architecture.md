# Architecture Notes

## Why Redis ZSet + Kafka

Redis ZSet efficiently answers “which tasks are due now”. Kafka is intentionally placed *after* the delay stage so that publishing work can scale independently and can use consumer groups, retries and DLT.

## Why not ZREM then Kafka send

Deleting the only delayed task before the Kafka broker acknowledges the record creates a loss window. This project therefore claims tasks into a `processing` ZSet with a lease. A task is removed only after Kafka ACK; otherwise it is requeued or recovered after lease expiry.

## Idempotency predicate

The consumer publishes with one atomic database statement equivalent to:

```sql
UPDATE article
SET status = 'PUBLISHED', published_at = :now
WHERE id = :articleId
  AND status = 'SCHEDULED'
  AND schedule_version = :scheduleVersion
  AND publish_time <= :now;
```

A return value of `0` is expected for duplicate or stale messages and is treated as success/no-op.

## Scaling

Multiple application instances may run the dispatcher. The Lua claim script is atomic inside Redis, so the same Redis member cannot be claimed by two instances at the same time. A global distributed scheduler lock is therefore unnecessary for correctness.

## React 管理端

```text
Browser
  │
  │ http://localhost:3000
  v
Nginx / React Admin
  │
  │ /api/* reverse proxy
  v
Spring Boot :8080
  │
  ├── GET  /api/articles
  ├── POST /api/articles
  ├── GET  /api/articles/{id}
  ├── POST /api/articles/{id}/schedule
  └── POST /api/articles/{id}/cancel-schedule
```

React 管理端不直接连接 Redis、Kafka 或 MySQL，所有业务操作均通过 Spring Boot API 完成，避免前端绕过领域校验与幂等规则。

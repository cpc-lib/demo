# SPEC — 通用请求幂等

## Goal

同一个逻辑命令允许被传输多次，但业务副作用最多成功一次，并可回放第一次成功结果。

## Request Contract

```http
Idempotency-Key: <8..128 chars>
```

- 同一逻辑命令的 retry 必须相同。
- 新逻辑命令必须使用新 Key。
- `scope = tenantId + ":" + operation`。
- 数据库只存 `SHA256(key)`，默认不存原始 Key。

## Payload Binding

```text
request_hash = SHA256(canonical-json(request))
```

相同 Key + 不同 request_hash：

```http
409 IDEMPOTENCY_KEY_REUSED
```

## State

```text
PROCESSING -> SUCCESS
```

普通业务失败不持久化 FAILED：事务整体 rollback，允许 retry。

## Local Transaction

以下必须在同一个事务中：

1. INSERT PROCESSING；
2. 业务写；
3. UPDATE SUCCESS + response。

## Redis

只用于并发快挡。

Redis 不可用：

```text
degrade to DB unique constraint
```

不得把 Redis 当唯一正确性来源。

## Concurrent Request

1. Redis lock 获取失败；
2. 短轮询 SUCCESS；
3. 成功则 replay；
4. 超时返回 `IDEMPOTENCY_REQUEST_IN_PROGRESS`。

## Retry Propagation

- Nginx：保持原 Key。
- Gateway：保持原 Key；缺失时仅做兼容补 Key。
- Feign：显式转发原 Key。

一个父请求要产生多个独立子命令时，不应盲目共用同一 Key，可派生：

```text
childKey = SHA256(parentKey + operation + logicalSubCommandId)
```

## Crash Semantics

- COMMIT 前 crash：rollback，retry 重新执行。
- COMMIT 后响应丢失：retry replay SUCCESS。
- Redis down：DB unique 保证正确性。
- Redis lock 过期：DB unique 继续兜底。

## Observability

至少统计：

```text
idem_first_total
idem_replay_total
idem_conflict_total
idem_in_progress_total
idem_redis_degraded_total
```

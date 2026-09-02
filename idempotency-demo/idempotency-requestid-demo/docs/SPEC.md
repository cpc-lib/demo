# SPEC — 后端签发 RequestId 的通用请求幂等

## Goal

用户的一次逻辑提交允许被 Nginx / Gateway / Feign 传输或重试多次，但业务副作用最多成功一次，并且重复请求返回第一次成功结果。

## Why backend-issued

关键写操作不让 Gateway 在业务请求到达时临时生成 Key，而采用两阶段协议：

```text
1. Client -> POST /api/idempotency/order-create
2. Backend -> requestId
3. Client -> POST /api/orders + Idempotency-Key=requestId
4. Nginx/Gateway/Feign retry -> same key
```

因此 requestId 在业务副作用发生之前就已存在。

## Important terminology

```text
X-Request-Id
= trace/log correlation id

Idempotency-Key
= logical business command id
```

两者不得混用。

## Token issue contract

```http
POST /api/idempotency/order-create
X-Tenant-Id: demo
```

Response:

```json
{
  "requestId": "idem_xxx",
  "expiresAt": "..."
}
```

- Token 由后端使用高随机 UUID 生成。
- DB 只保存 `SHA256(token)`。
- Token 绑定服务端生成的 scope：`tenantId:order:create`。
- 未使用 Token 默认 10 分钟过期。
- SUCCESS 记录的保留时间独立于签发 TTL，由清理策略控制。

## Command contract

```http
POST /api/orders
Idempotency-Key: <backend-issued requestId>
```

缺失：Gateway 返回 `428 IDEMPOTENCY_KEY_REQUIRED`。

## Payload binding

```text
request_hash = SHA256(canonical-json(body))
```

同一个 token + 不同 payload：

```http
409 IDEMPOTENCY_KEY_REUSED
```

## Database state

```text
ISSUED -> PROCESSING -> SUCCESS
```

`PROCESSING` 与业务写发生在同一个 DB 事务中；事务回滚后状态恢复为 ISSUED。

## Concurrency algorithm

```text
BEGIN
  SELECT idempotency_token ... FOR UPDATE
  validate scope/token/request_hash

  if SUCCESS:
      replay response
      COMMIT

  mark PROCESSING + request_hash
  write business data
  mark SUCCESS + response_json
COMMIT
```

并发的第二个相同请求会等待同一 token 行锁；第一个提交后，它读取 SUCCESS 并回放，不会重复执行业务。

## Crash semantics

- COMMIT 前进程崩溃：数据库 rollback，Token 仍为 ISSUED，retry 可重新执行。
- COMMIT 后响应丢失：retry 读取 SUCCESS 并回放。
- Feign/Gateway 重试：必须原样透传 Idempotency-Key。
- Nginx upstream retry：必须原样透传 Idempotency-Key。

## Nginx boundary

Nginx 的 `$request_id` 可作为 trace id，并在一次 Nginx 请求的 upstream retry 中保持稳定；但它不能替代业务 Idempotency-Key，因为客户端超时后重新发起的是一个新的 Nginx request。

## External side effects

DB 事务不能覆盖 MQ / 支付 / 邮件 / 第三方 HTTP。必须继续使用：

```text
business tx + outbox event
       ↓
MQ at-least-once
       ↓
consumer eventId idempotency
```

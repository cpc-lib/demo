# Acceptance Criteria

| ID | 场景 | 预期 |
|---|---|---|
| AC-01 | 第一次请求 | 业务执行一次，记录 SUCCESS |
| AC-02 | 同 Key 同 payload 重试 N 次 | 返回同一结果，业务 count=1 |
| AC-03 | 同 Key 不同 payload | 409 `IDEMPOTENCY_KEY_REUSED` |
| AC-04 | 第一次业务异常 | 全事务 rollback，后续允许重试 |
| AC-05 | Redis down | DB unique 仍保证最多一次 |
| AC-06 | COMMIT 后响应 503 | retry replay，业务 count=1 |
| AC-07 | 20 个并发同 Key | 最终最多一条业务记录 |
| AC-08 | Client→Nginx→Gateway→Feign | Key 全链路不变化 |

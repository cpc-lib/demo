# TDD Plan

```text
SPEC
 ↓
Acceptance Criteria
 ↓
RED
 ↓
GREEN
 ↓
REFACTOR
 ↓
Integration/E2E
```

## RED 1 — Core

先写：

- `sameKeySamePayloadShouldReplayWithoutExecutingTwice`
- `sameKeyDifferentPayloadShouldReject`
- `failedBusinessTransactionShouldNotCacheSuccess`

再实现 `IdempotencyTemplate`。

## RED 2 — Business

先写：

- `sameRequestShouldCreateOnlyOneOrderAndReplay`

再实现 `OrderApplicationService`。

## Integration

验证：

1. 普通重复提交；
2. Gateway 首次下游成功后故意 500；
3. Feign 首次 DB 已提交后故意 503；
4. Redis 停机后重复请求；
5. 并发同 Key。

最终验收：

```sql
SELECT COUNT(*)
FROM biz_order
WHERE tenant_id='demo'
  AND idempotency_key_hash=SHA2('<key>',256);
```

结果必须为 `1`。

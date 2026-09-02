# Acceptance Criteria

1. 后端能签发 `requestId`，有效期默认 10 分钟。
2. `POST /api/orders` 未携带 Idempotency-Key 时返回 HTTP 428。
3. 同一后端签发 requestId + 相同 payload 调用 10 次：数据库只创建 1 条订单，后续返回相同 orderNo 且 `replayed=true`。
4. 同一 requestId + 不同 payload：返回 409，不产生第二条订单。
5. Token 不允许跨 tenant / operation 使用。
6. 第一次业务事务异常 rollback 后，相同 requestId 可以重试成功。
7. 第一次订单事务已 COMMIT、HTTP 503 导致 Feign retry：仍只有 1 条订单。
8. caller 已调用 order 成功但返回 500 导致 Gateway POST retry：仍只有 1 条订单。
9. `X-Request-Id` 仅用于 tracing，不作为幂等判断依据。
10. 原始 requestId 不写数据库，只存 SHA-256。

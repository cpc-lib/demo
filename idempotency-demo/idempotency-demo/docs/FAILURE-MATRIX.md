# Failure Matrix

| Failure | 可能重复请求 | 保护机制 |
|---|---:|---|
| 用户双击 | 是 | Key + DB UNIQUE |
| Browser/App timeout retry | 是 | SUCCESS replay |
| Nginx retry | 是 | Key 透传 + 服务端幂等 |
| Gateway POST Retry | 是 | Key 不变 + 服务端幂等 |
| Feign Retry | 是 | Key 不变 + 服务端幂等 |
| Redis down | 是 | DB UNIQUE |
| Redis lease 到期 | 是 | DB UNIQUE |
| COMMIT 前 crash | 是 | rollback 后重新执行 |
| COMMIT 后响应丢失 | 是 | SUCCESS replay |
| MQ 重投 | 是 | Consumer eventId 幂等（独立机制） |
| 支付渠道 timeout | 是 | 渠道业务幂等号 + 状态查询 |

# Failure Matrix

| Failure | Expected behavior |
|---|---|
| Duplicate click using same issued token | replay first SUCCESS |
| Gateway POST retry | same Idempotency-Key, replay |
| Feign retry | same Idempotency-Key, replay |
| Nginx upstream retry | preserve header, replay |
| Process crash before commit | rollback to ISSUED; retry executes |
| Response lost after commit | SUCCESS exists; retry replays |
| Same token, different body | 409 conflict |
| Token used by another tenant/operation | 409 scope mismatch |
| Unknown fabricated token | 409 token not found |
| Unused token expires | reject; client obtains a new token |
| Redis unavailable | correctness unaffected in this variant; DB row lock is authoritative |
| External MQ duplicate | consumer must use eventId idempotency separately |

# TDD Test Plan

## RED 1 — token issue + replay

- issue token
- execute command twice
- expect action called once and second result replayed

## RED 2 — payload binding

- same token
- first payload=A success
- retry payload=B
- expect `IDEMPOTENCY_KEY_REUSED`

## RED 3 — rollback semantics

- issue token
- action throws after PROCESSING is written
- transaction rollback
- retry same token succeeds

## RED 4 — scope isolation

- tenant-a issues token
- tenant-b tries to consume it
- expect `IDEMPOTENCY_TOKEN_SCOPE_MISMATCH`

## RED 5 — order integration

- backend issue requestId
- create order twice
- expect same orderNo and DB count=1

## E2E fault injection

- Gateway retry after caller returns 500 once
- Feign retry after order-service commits then returns 503 once
- repeated curl/PowerShell submits using same server-issued requestId

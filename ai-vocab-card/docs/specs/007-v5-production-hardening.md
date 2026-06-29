# 007 - V5 Production Hardening Specification

## 1. Goal

V5 upgrades AI Vocabulary Studio from feature-complete learning product to production-oriented platform.

## 2. Scope

- Vector search provider abstraction with Milvus REST adapter boundary.
- Outbox event table for DB/MQ consistency.
- Redis-based rate limiting and idempotency guards.
- Export task list and retry operations.
- Spring Boot Actuator health/info/metrics observability endpoints.
- Ops capability endpoint.

## 3. Acceptance Criteria

- Saving a word can remain available even when vector/MQ side effects are temporarily unavailable.
- Duplicate save requests with the same `Idempotency-Key` are rejected within the configured TTL.
- AI generation endpoint is rate-limited by user and minute.
- Outbox failed events are retried using exponential backoff.
- Export tasks can be listed and failed tasks can be retried.
- `/actuator/health`、`/actuator/info`、`/actuator/metrics` expose basic operational endpoints.

## 4. Non-goals

- The bundled Milvus path is an adapter boundary. A production Milvus deployment or vector adapter service can be attached without changing application services.

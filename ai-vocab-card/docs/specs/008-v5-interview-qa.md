# V5 面试问答

## Q1：为什么引入 Outbox？

因为数据库提交和 MQ 投递不是一个原子操作。直接在事务里发 MQ 可能出现 DB 成功但 MQ 失败，或者 MQ 成功但事务回滚。Outbox 先把领域事件和业务数据写入同一个数据库事务，再由后台任务可靠投递 MQ，通过状态和重试保证最终一致性。

## Q2：为什么语义搜索不直接写死 Milvus SDK？

为了降低业务层和具体向量库实现的耦合。V5 通过 `VectorSearchProvider` 定义边界，默认使用本地降级检索，生产可开启 Milvus REST adapter。以后换 pgvector、Qdrant、Elasticsearch dense_vector 时，不影响 Controller 和应用服务。

## Q3：如何防止用户重复点击保存？

V5 支持 `Idempotency-Key`。前端每次提交生成一个唯一 key，后端用 Redis `SETNX + TTL` 做幂等保护。同一用户、同一接口、同一 key 在 TTL 内只能成功一次。

## Q4：如何做接口限流？

通过 `@RateLimited` 注解和 Redis 计数器按用户、接口、分钟维度限流。它适合保护 AI 生成接口，防止大模型成本失控。

## Q5：V5 的可观测性怎么做？

当前版本保留 Spring Boot Actuator 的 `/actuator/health`、`/actuator/info`、`/actuator/metrics` 基础运维端点。后续如需要生产监控，可接入 OpenTelemetry 或其他监控系统。

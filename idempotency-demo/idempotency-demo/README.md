# Spring Cloud 通用请求幂等 Demo（SDD + TDD）

目标：即使 **客户端、Nginx、Gateway、Feign** 因超时/5xx发生重试，同一个业务命令也只产生一次业务副作用。

## 1. 核心架构

```text
Client
  │ Idempotency-Key（同一逻辑命令永远复用）
  ▼
Nginx
  │ 原样透传
  ▼
Spring Cloud Gateway
  │ POST Retry（Demo 故意开启）
  ▼
caller-service
  │ OpenFeign Retry（Demo 只对 503 开启）
  ▼
order-service
  │
  ├─ request_hash：防 Key 被不同 payload 复用
  ├─ Redis SETNX：并发快挡，可降级
  ├─ MySQL UNIQUE(scope,key_hash)：最终正确性
  └─ 本地事务：idem record + biz_order + SUCCESS response
```

**关键原则：**

```text
At-Least-Once Transport + Business Idempotency ≈ Exactly-Once Effect
```

不要依赖“关闭重试”保证安全；服务端必须具备幂等能力。

## 2. 版本基线

- JDK 21
- Spring Boot 3.5.0
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2025.0.0.0
- Nacos 3.0.3
- MySQL 8.4
- Redis 7.4
- Nginx stable

这组 Spring Cloud Alibaba / Spring Cloud / Spring Boot / Nacos 版本采用官方 2025.0.x 对应矩阵。

## 3. Key 规则

最佳做法是在**最早的重试边界之前**生成：

```text
Browser/App/Caller
   ↓ Key=A
Nginx
   ↓ Key=A
Gateway
   ↓ Key=A
Feign
   ↓ Key=A
Order Service
```

如果 Nginx 位于 Gateway 前面，而 Key 到 Gateway 才生成，则 Nginx 重新发起请求时会得到新 Key，保护不了这一层重试。因此真正的创建/支付类命令最好由前端或最上游调用方生成。

Demo 中 Gateway 会在缺失时补一个 UUID，仅用于兼容；生产关键写操作建议要求客户端必传。

## 4. 数据模型

`idempotency_record`：

```text
scope
key_hash        = SHA256(Idempotency-Key)
request_hash    = SHA256(canonical-json(request))
status          = PROCESSING/SUCCESS
response_json
business_ref
UNIQUE(scope,key_hash)
```

同 Key、不同 payload：

```text
409 IDEMPOTENCY_KEY_REUSED
```

同 Key、同 payload、已成功：

```text
不再执行 order INSERT
直接回放第一次 response_json
```

## 5. 为什么 Redis 不能作为最终保证

Redis 只做：

```text
SET idem:lock:* token NX PX 30s
```

它可能：

- 故障；
- 重启；
- 锁 lease 到期；
- 发生网络分区。

因此 Redis 故障时本 Demo **降级到 MySQL**。最终正确性依靠：

```sql
UNIQUE(scope, key_hash)
```

同时 `biz_order` 还有 `(tenant_id,idempotency_key_hash)` 唯一约束作为第二道保险。

## 6. 最关键事务边界

```text
BEGIN
  INSERT idempotency_record(PROCESSING)
  INSERT biz_order(...)
  UPDATE idempotency_record
     SET status=SUCCESS,response_json=...
COMMIT
```

如果业务抛异常：

```text
ROLLBACK
```

`PROCESSING` 也回滚，retry 可以重新执行。

如果 COMMIT 成功，但 HTTP 响应丢失：

```text
调用方 timeout
   ↓ retry same key
SUCCESS exists
   ↓
回放第一次结果
```

这就是本方案真正解决的事故场景。

## 7. 启动基础设施与前端

基础设施（MySQL/Redis/Nacos/Nginx）在 `idempotency-env/`：

```bash
cd idempotency-env
docker compose up -d
cd ..
```

构建前端控制台（Vue 3，产物 `frontend/dist` 由 Nginx 托管）：

```bash
cd frontend
npm install
npm run build
cd ..
```

端口：

- MySQL `3306`
- Redis `6379`
- Nacos client `8848`
- Nacos Console `8088`
- Nginx `80`（前端控制台 + API 入口，浏览器打开 http://localhost）

前端开发模式（热更新，`/api` 代理到 Gateway `8080`；想走完整 Nginx 链路可设 `API_PROXY_TARGET=http://localhost`）：

```bash
cd frontend
npm run dev      # http://localhost:5173
```

然后：

```bash
mvn clean test
mvn clean package -DskipTests
```

分别启动：

```bash
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar
java -jar caller-service/target/caller-service-1.0.0-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar
```

## 8. 普通重复请求

```bash
curl -i -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-request-000001" \
  -d '{"userId":1001,"itemName":"keyboard","amount":299.00}'
```

重复执行多次，应始终得到相同 `orderNo`，数据库只有一条订单。

## 9. 模拟 Gateway Retry

```bash
curl -i -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: gateway-retry-000001" \
  -H "X-Demo-Gateway-Fail-Once: true" \
  -d '{"userId":1001,"itemName":"monitor","amount":1999.00}'
```

第一次链路中：

```text
caller -> order-service 创建成功
caller 故意返回 500
Gateway retry POST
caller 再次调用 order-service
order-service replay
```

最终只有一条订单。

## 10. 模拟 Feign Retry

```bash
curl -i -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: feign-retry-000001" \
  -H "X-Demo-Feign-Fail-Once: true" \
  -d '{"userId":1001,"itemName":"mouse","amount":199.00}'
```

order-service 第一次：

```text
DB transaction COMMIT
Controller 故意返回 503
```

Feign 把 503 识别成 RetryableException 后重试，同一个 Key 再次进入 order-service，直接 replay。

## 11. Nginx

生产配置**不要轻易**对 POST/PATCH 开启：

```nginx
non_idempotent
```

本 Demo nginx.conf 保留安全默认行为，并始终透传 `Idempotency-Key`。

即便未来因为业务原因开启 non-idempotent retry，后端幂等层仍作为最后保护。

## 12. 外部副作用边界

数据库事务不能自动解决：

- MQ 重投；
- 支付渠道；
- 短信/邮件；
- 另一个远程系统。

推荐：

```text
local tx:
  business
  idempotency_record
  outbox_event
COMMIT
   ↓
outbox publisher
   ↓
MQ
   ↓
consumer uses eventId idempotency
```

支付/退款则继续向渠道传业务幂等号。

## 13. SDD/TDD

见：

- `docs/SPEC.md`
- `docs/ACCEPTANCE-CRITERIA.md`
- `docs/TEST-PLAN.md`
- `docs/FAILURE-MATRIX.md`

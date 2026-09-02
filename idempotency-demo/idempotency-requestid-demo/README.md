# Spring Cloud 后端签发 RequestId 幂等 Demo（SDD + TDD）

这是上一版“客户端传 Idempotency-Key”的**后端签发版**。

目标：用户一次逻辑提交即使被 **Nginx / Spring Cloud Gateway / OpenFeign** 重试多次，订单仍只创建一次，并且 retry 返回第一次成功结果。

## 1. 为什么不是 Gateway 收到 POST 时才生成 requestId

如果链路是：

```text
Browser -> Nginx -> Gateway -> Service
```

而 Gateway 在 `/orders` 到达时才生成 Key，那么位于 Gateway 前面的重试边界无法稳定复用这个 Key。

本 Demo 使用：

```text
Client
  │
  │ ① POST /api/idempotency/order-create
  ▼
Backend
  │
  │ ② requestId = idem_xxx
  ▼
Client
  │
  │ ③ POST /api/orders
  │    Idempotency-Key: idem_xxx
  ▼
Nginx -> Gateway -> caller-service -> Feign -> order-service
                 (任何 retry 都保持同一个 Key)
```

所以**后端生成**并不等于“业务请求到了 Gateway 后再生成”，而是“业务提交之前先向后端申请一个 token”。

## 2. RequestId 与 TraceId 必须分开

```text
X-Request-Id
= 链路追踪、日志关联

Idempotency-Key
= 业务幂等、Exactly-once effect
```

Nginx/Gateway 可以自动生成 `X-Request-Id`；但创建订单、支付、退款等关键写操作必须使用稳定的 `Idempotency-Key`。

## 3. 核心实现

数据库提前存在一条 token：

```text
idempotency_token
  token_hash   = SHA256(raw requestId)
  scope        = demo:order:create
  status       = ISSUED
  expires_at   = now + 10min
```

业务请求：

```text
BEGIN
  SELECT token FOR UPDATE

  if SUCCESS:
      replay response

  validate scope
  validate request_hash

  status = PROCESSING
  INSERT biz_order
  status = SUCCESS
  response_json = first response
COMMIT
```

`SELECT ... FOR UPDATE` 是这里的关键：两个重复请求同时进来，只能有一个持有 token 行锁；另一个等待后读取 SUCCESS 并 replay。

## 4. 为什么这个方案比“Redis SETNX 后删除”更稳

Redis 锁只能解决“同时进入”。最难的问题是：

```text
DB COMMIT 成功
      ↓
HTTP response 丢失
      ↓
Feign/Gateway retry
```

本方案把：

```text
业务数据 + token SUCCESS + response_json
```

放在**同一个 MySQL 事务**里，所以 retry 能确定第一次到底有没有成功，并返回原结果。

Redis 可以以后作为并发快挡加上，但不是 correctness source。

## 5. 环境

- JDK 21
- Spring Boot 3.5.0
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2025.0.0.0
- Nacos 3.0.3
- MySQL 8.4
- Redis 7.4（保留在环境中，方便扩展 fast guard / cache）
- Nginx stable

启动基础设施：

```bash
docker compose up -d
```

端口：

```text
MySQL        3306
Redis        6379
Nacos        8848
Nacos UI     8088
Nginx        80
Gateway      8080
```

构建：

```bash
mvn clean test
mvn clean package -DskipTests
```

启动：

```bash
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar
java -jar caller-service/target/caller-service-1.0.0-SNAPSHOT.jar
java -jar gateway-service/target/gateway-service-1.0.0-SNAPSHOT.jar
```

## 6. 第一步：后端申请 requestId

```bash
curl -X POST http://localhost/api/idempotency/order-create \
  -H "X-Tenant-Id: demo"
```

返回：

```json
{
  "requestId": "idem_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "expiresAt": "2026-08-27T...Z"
}
```

## 7. 第二步：使用 requestId 创建订单

```bash
curl -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -H "Idempotency-Key: idem_xxx" \
  -d '{"userId":1001,"itemName":"keyboard","amount":299.00}'
```

重复 10 次同一个请求，只有第一条会真正 `INSERT biz_order`。

也可以直接运行：

```bash
./scripts/demo.sh
```

Windows PowerShell：

```powershell
./scripts/demo.ps1
```

### 前端测试页面（Vue）

以上两步及全部验收场景都可在 `frontend/` 的测试控制台中一键执行：

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173（Vite 把 /api 代理到 Nginx:80）
npm run build   # 产物 frontend/dist
```

本机原生 nginx 测试（**项目自带独立配置**，不改动 nginx 安装目录的 `conf/nginx.conf`，原理是 `nginx -p <nginx目录> -c frontend/nginx.conf`）：

```powershell
cd frontend
npm run nginx:test    # 校验 frontend/nginx.conf（nginx -t）
npm run nginx:start   # 启动：http://localhost/ （/api 反代到 Gateway:8080）
npm run nginx:reload  # 修改配置后热重载
npm run nginx:quit    # 停止
```

nginx 安装目录不是 `d:\develop\nginx-1.30.4` 时：`powershell -ExecutionPolicy Bypass -File nginx.ps1 start -NginxHome <nginx目录>`。项目挪动位置后需同步修改 `frontend/nginx.conf` 里的 `root` 路径。

页面内置 10 个自动化验收场景（正常创建、同 Key 并发重复提交 ×10、缺失 Key 428、同 Key 换 payload 409、跨租户 409、伪造 token、非法长度 Key、参数校验 400、COMMIT 后 503 的 Feign 重试、caller 500 的 Gateway 重试），并提供：

- 四条链路切换：Nginx 全链路 / 直连 Gateway / 直连 caller-service / 直连 order-service（后两者需 `npm run dev` 的 Vite 代理，可观察纯净的 409 错误体与无 Gateway 拦截的差异）；
- requestId 管理面板（TTL 倒计时、选中自动填入表单）；
- 手动提交面板（可编辑 Idempotency-Key、开关两个故障注入头）；
- 订单汇总（多次成功提交收敛到同一 orderNo，验证至多一次落库）；
- 请求日志（对比每次可变的 `X-Request-Id` 与恒定回显的 `Idempotency-Key`）。

## 8. Gateway Retry 故障注入

先申请新 token，然后：

```bash
curl -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: <requestId>" \
  -H "X-Demo-Gateway-Fail-Once: true" \
  -d '{"userId":1001,"itemName":"monitor","amount":1999.00}'
```

流程：

```text
Gateway -> caller -> order
                  order COMMIT ✅
caller 故意 500
Gateway retry POST
caller -> order
          SUCCESS replay ✅
```

## 9. Feign Retry 故障注入

```bash
curl -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: <requestId>" \
  -H "X-Demo-Feign-Fail-Once: true" \
  -d '{"userId":1001,"itemName":"mouse","amount":199.00}'
```

order-service 第一次 COMMIT 后故意返回 503，Feign retry 使用相同 header，再次进入后 replay。

## 10. Nginx

配置明确分开：

```nginx
proxy_set_header X-Request-Id $request_id;
proxy_set_header Idempotency-Key $http_idempotency_key;
```

`$request_id` 只负责 tracing。

业务 Idempotency-Key 必须保留后端预签发的值；不要在 retry 时生成新值。

## 11. 生产建议

- token 签发 scope 必须由服务端白名单决定，不能让客户端自由拼接权限敏感 scope；
- unused token TTL：5~15 分钟通常足够；
- SUCCESS replay 记录根据业务保留 24h / 7d / 账务周期；
- 定时清理过期 ISSUED 和超过保留期 SUCCESS；
- 支付/退款优先使用 `paymentNo/refundNo` 这种天然业务幂等号；
- MQ 使用 `eventId` + consumer inbox/unique key；
- 外部副作用使用 transactional outbox，不要认为 DB token 能覆盖第三方调用。

## 12. SDD / TDD

- `docs/SPEC.md`
- `docs/ACCEPTANCE-CRITERIA.md`
- `docs/TEST-PLAN.md`
- `docs/FAILURE-MATRIX.md`

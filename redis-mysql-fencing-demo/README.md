# Redis Cluster + MySQL Fencing Token Demo

> Redis Cluster 分布式锁 + MySQL Fencing Token 栅栏令牌 — 解决旧锁持有者在 JVM 暂停 / 网络分区后恢复时仍能写入的一致性问题。

## 架构

```
浏览器 (React 测试面板)
  │
  ▼  Vite 代理 /api → :8080
Spring Boot (本机, port 8080)
  │
  ├── Redisson Cluster  ──►  Redis 7.4 × 6 (3M + 3R, bridge 网络)
  │         │                         │
  │         │  NatMapper: bridge IP ──► 127.0.0.1
  │         │                         │
  │         └── 端口映射 7000-7005  ──┘
  │
  └── MySQL 8.4 (localhost:3306, Docker 端口映射)
```

**为什么需要 NatMapper？** Windows Docker Desktop 用 bridge 网络，容器间互通用 172.20.x.x 这样的内部 IP。节点 announce 内部 IP 才能 Gossip 正常组成 3 节点集群；但 Windows 上的 Spring Boot 不在这个 bridge 网络里，需要走 `localhost:7000` 端口映射。Redisson `CLUSTER NODES` 读到内部 IP 后，通过 NatMapper 统一改写成 `127.0.0.1` 再连接。

### Fencing Token 时序

```
A 尝试加锁 → Redis 成功, fence_token=101
A 暂停 (GC/网络), 锁过期
B 加锁成功, fence_token=102
A 恢复 → UPDATE ... WHERE fence_token=101 → 影响行数 0 (被 MySQL 拒绝)
B 写入 → UPDATE ... WHERE fence_token=102 → 影响行数 1
```

MySQL 行锁串行化 Token 切换和业务写入，旧持有者无法覆盖新数据。

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端 | Spring Boot / Redisson / Flyway | 3.5.16 / 4.8.0 / 11 |
| Redis | 3 Master + 3 Replica Cluster | 7.4 |
| MySQL | InnoDB, caching_sha2_password | 8.4 |
| 前端 | React + TypeScript + Vite + Tailwind | React 18 / Vite 6 |
| 容器 | Docker Compose (bridge 网络) | — |
| 语言 | Java 21 / Node 20+ | — |

## 目录

```
redis-fencing-single-host/
├── docker-compose.yml              # 基础环境：Redis Cluster (3M + 3R) + MySQL + init
├── .env                            # 唯一配置源：Compose + Spring Boot 共用
├── .env.example                    # 不含密钥的模板
├── scripts/
│   ├── init-cluster.sh             # 创建集群, 校验 16384 slots
│   └── check.sh                    # 检查集群状态
├── pom.xml                         # Maven 元数据
├── src/main/java/com/example/fencing/
│   ├── FencingApplication.java
│   ├── config/RedisConfiguration.java      # Redisson + NatMapper
│   ├── service/                            # 锁协调 / Token 发号 / 业务更新
│   ├── repository/                         # Counter / Operation CRUD
│   ├── web/CounterController.java          # HTTP API
│   ├── model/                              # 请求响应 DTO
│   └── error/                              # 异常类型
├── src/main/resources/
│   ├── application.yml                     # 后端配置
│   └── db/migration/V1__init.sql           # Flyway 初始化
├── frontend/                       # React 测试面板 (Vite)
│   ├── src/{App.tsx, api.ts, main.tsx}
│   └── package.json
└── docs/
    ├── DEPLOY_SINGLE_HOST.md       # Windows / Linux 部署详解
    └── reference/                 # 技术笔记与设计文档
```

## 快速启动 (Windows Docker Desktop)

### 1. 生成密码

```powershell
cd d:\release\temp\redis-fencing-single-host

# 密码生成 (PowerShell 原生):
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
function New-Hex([int]$bytes=32) { $b = New-Object byte[] $bytes; $rng.GetBytes($b); ($b | ForEach-Object { $_.ToString('x2') }) -join '' }

Copy-Item .env.example .env
# 打开 .env, 把空密码替换成 (New-Hex 生成的 64 位 hex):
# REDIS_PASSWORD=
# MYSQL_ROOT_PASSWORD=
# MYSQL_APP_PASSWORD=
# DB_PASSWORD=            (必须与 MYSQL_APP_PASSWORD 相同)
```

`.env` 是**唯一配置源**：Docker Compose 和 Spring Boot 都读它。`application.yml` 里的 `${DB_URL}`、`${REDIS_PASSWORD}` 等占位符与 `.env` 的 key 一一对应。

### 2. 启动基础设施

> 以下命令均在项目根目录执行。

```powershell
docker compose config --quiet       # 校验 compose
docker compose up -d                # 启动 Redis × 6 + MySQL + init
docker compose ps                   # 等待所有 healthy
```

验证集群:

```powershell
docker compose exec -T redis-7000 sh -c 'REDISCLI_AUTH=$REDIS_PASSWORD redis-cli cluster info'
# 预期: cluster_state:ok / cluster_slots_assigned:16384 / cluster_known_nodes:6
```

### 3. 后端

```powershell
cd d:\release\temp\redis-fencing-single-host

mvn.cmd spring-boot:run
# 看到 Started FencingApplication in X seconds = 成功
# 端口 8080
```

Spring Boot 启动时通过 `spring.config.import` 自动读取根目录的 `.env`，**无需事先设置任何系统环境变量**。配置项全部写在 `.env`，key 与 `application.yml` 一一对应。

### 4. 前端

```powershell
cd frontend
npm install
npm run dev     # http://localhost:5173
```

Vite 已配置代理 `/api` → `http://localhost:8080`。登录后从数据库商品列表中选择商品，测试抢购 / 并发压测 / 重置数据。

### 测试账号

前端登录（`POST /api/auth/login`）使用以下硬编码测试账号，密码统一为 `123456`：

| 用户名 | userId | 密码 |
|---|---|---|
| alice | alice | 123456 |
| bob | bob | 123456 |
| charlie | charlie | 123456 |
| dave | dave | 123456 |

登录返回 `{ token, userId, expiresInMs }`（JWT，默认 2 小时有效）。除 `/api/auth/login`、`/api/counters/**`、`/actuator/**` 外，其余接口都必须带 `Authorization: Bearer <token>`。

### 5. 一键 API 测试

后端启动后:

```powershell
$h = @{ "Idempotency-Key" = "test_001" }
Invoke-RestMethod -Uri 'http://localhost:8080/api/counters/demo/add' -Method Post -Headers $h `
  -Body '{"delta":1}' -ContentType 'application/json' | ConvertTo-Json

# 预期: {"resourceId":"demo","delta":1,"value":1,"fenceToken":1,"replayed":false}
```

## API

| 方法 | 路径 | Header | Body | 说明 |
|---|---|---|---|---|
| `POST` | `/api/counters/{resourceId}/add` | `Idempotency-Key` | `{"delta": N}` | 原子加 N, 发新 Token |
| `GET` | `/api/counters/{resourceId}` | — | — | 读取 value + fenceToken |

响应:

```json
{
  "resourceId": "demo",
  "delta": 1,
  "value": 1,
  "fenceToken": 1,
  "replayed": false
}
```

| 状态码 | 含义 |
|---|---|
| `200 OK` | 正常更新 |
| `400 BAD_REQUEST` | 参数校验失败 |
| `409 IDEMPOTENCY_CONFLICT` | 同 Key 不同 delta 冲突 |
| `409 STALE_FENCE` | 旧 Token 被拒绝 (由数据库行锁保证) |
| `423 LOCK_BUSY` | Redis 锁获取超时 |

## 秒杀 API

| 方法 | 路径 | Header | 说明 |
|---|---|---|---|
| `POST` | `/api/auth/login` | — | 登录，body `{"username":"alice","password":"123456"}`，返回 JWT |
| `GET` | `/api/seckill` | `Authorization: Bearer <jwt>` | 数据库商品列表 |
| `GET` | `/api/seckill/{productId}` | `Authorization: Bearer <jwt>` | 单个商品库存 / 已售 / fence_token |
| `POST` | `/api/seckill/{productId}/buy` | `Authorization` + `Idempotency-Key` | 秒杀下单（一人一单、不超卖） |
| `POST` | `/api/seckill/reset` | `Authorization: Bearer <jwt>` | 重置全部秒杀数据 |

种子商品（Flyway V2 / V3）：`SKU-001` 秒杀旗舰款 10 件、`SKU-002` 秒杀轻薄本 14 寸 5 件、`SKU-003` 秒杀降噪耳机 20 件、`SKU-004` 秒杀智能手表 8 件。

重置语义：清空 `seckill_order` / `seckill_operation`，库存恢复为 `initial_stock`、`sold` 归零，`fence_token` 保持单调不回退；事务提交后清除 Redis 商品缓存（下次请求重新回源 DB）。

```powershell
$login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post `
  -Body '{"username":"alice","password":"123456"}' -ContentType 'application/json'
$h = @{ Authorization = "Bearer $($login.token)"; 'Idempotency-Key' = 'test_001' }

Invoke-RestMethod -Uri 'http://localhost:8080/api/seckill/SKU-001/buy' -Method Post -Headers $h | ConvertTo-Json
# 预期: {"productId":"SKU-001","userId":"alice","orderId":1,"fenceToken":1,"remainingStock":9,"replayed":false}

Invoke-RestMethod -Uri 'http://localhost:8080/api/seckill/reset' -Method Post -Headers @{ Authorization = "Bearer $($login.token)" } | ConvertTo-Json
# 预期: {"products":4,"orders":1,"operations":1}
```

秒杀错误码：`404 PRODUCT_NOT_FOUND`、`409 ALREADY_PURCHASED`（一人一单）、`410 STOCK_OUT`（售罄）、`423 LOCK_BUSY`（锁竞争）、`429 RATE_LIMITED`（商品级 / 用户级限流）。

## Windows vs Linux 差异

| 项目 | Windows Docker Desktop | 裸金属 Linux |
|---|---|---|
| 网络模式 | bridge (`host` 不支持) | `host` 或 bridge |
| announce IP | 容器 bridge IP, NatMapper 改写成 127.0.0.1 | 真实网卡 IP |
| 端口映射 | Redis × 6 + MySQL 全部映射到主机 | host 模式无需映射 |
| Spring Boot | 本机, 连 localhost | 本机或远程, 连内网 IP |

在 Linux 上把 bridge IP 直接用作 announce IP 也是可行的，只要客户端能访问这些 IP。

## 风险

- **单机 3M + 3R** — Replica 全部在同一台 Docker 宿主机上，宿主机故障 = 整体不可用。生产建议 Master 和 Replica 分散到不同物理机。
- **Fencing Token 依赖 MySQL 正确性** — 不要在有数据时 `docker compose down -v`、恢复旧备份、或让 Token 回退。
- **`.env` 含密钥** — 禁止上传公开仓库。gitignore 已配置。
- **服务绑 `0.0.0.0`** — 本机 Docker Desktop 没问题；暴露到公网前必须加防火墙。

详细部署与故障排查见 [docs/DEPLOY_SINGLE_HOST.md](docs/DEPLOY_SINGLE_HOST.md)。技术原理见 [docs/reference/](docs/reference/)。

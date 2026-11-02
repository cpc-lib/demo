# 单服务器部署详解

> 覆盖 **Windows Docker Desktop** 和 **裸金属 Linux Docker Host**。两者的差异主要在网络层，Redis Cluster 初始化和后端连接配置共用同一套 compose。

## 一、前置条件

| 依赖 | 版本 | 说明 |
|---|---|---|
| Docker | 27+ | Desktop 或 Engine |
| Docker Compose | v2 | Desktop 自带 |
| Java | 21 | 后端编译运行 |
| Maven | 3.9+ | `mvn.cmd` (Windows) 或 `mvn` (Linux) |
| Node.js | 20+ | 前端 |
| 可用端口 | 7000-7005, 17000-17005, 3306, 8080, 5173 | 无冲突 |

## 二、目录与密码

```bash
# 所有基础设施文件位于项目根目录
cd redis-fencing-single-host

# Linux/macOS
cp .env.example .env
openssl rand -hex 32 > /tmp/p1; openssl rand -hex 32 > /tmp/p2; openssl rand -hex 32 > /tmp/p3

# PowerShell
Copy-Item .env.example .env
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
function New-Hex([int]$bytes=32) { $b = New-Object byte[] $bytes; $rng.GetBytes($b); ($b | ForEach-Object { $_.ToString('x2') }) -join '' }
```

> **本文所有 `docker compose` 命令均需在项目根目录下执行。**

`.env` 需要填写的密钥值（64 个 hex 字符），其余配置项保持模板默认即可：

```dotenv
# 基础设施（Docker Compose）
MYSQL_ROOT_PASSWORD=<替换>
MYSQL_APP_PASSWORD=<替换>
MYSQL_HOST_PORT=3306
REDIS_PASSWORD=<替换>

# Spring Boot（application.yml 读取同名 key）
SERVER_PORT=8080
DB_URL=jdbc:mysql://127.0.0.1:3306/fencing_demo?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC
DB_USER=fencing_app
DB_PASSWORD=<替换，与 MYSQL_APP_PASSWORD 相同>
APP_REDIS_MODE=cluster
APP_REDIS_NODES=redis://127.0.0.1:7000,redis://127.0.0.1:7001,redis://127.0.0.1:7002
REDIS_LOCK_WATCHDOG_MS=30000
REDIS_LOCK_WAIT_SECONDS=2
```

> **旧版 .env 里的 `REDIS_ANNOUNCE_IP` 已废弃。** compose 里 Redis 启动时会动态 `getent hosts $(hostname)` 解析容器的 bridge IP 作为 announce 地址，不需要手动指定。

## 三、基础设施启动

### 3.1 Windows Docker Desktop

```powershell
docker compose config --quiet       # 校验 compose 渲染无错
docker compose up -d                # 启动 redis-7000~7005 + mysql + cluster-init
docker compose ps                   # 等待 8 个服务 healthy / exited
```

compose 使用 bridge 网络。Redis 绑定 `0.0.0.0` 并映射端口:

| 主机端口 | 容器端口 | 用途 |
|---|---|---|
| `7000-7005` | `7000` | Redis 节点 (3 Master + 3 Replica) |
| `17000-17005` | `17000` | Redis Cluster Bus |
| `3306` | `3306` | MySQL |

> Windows Docker Desktop **不支持 `network_mode: host`**，必须用 bridge 网络 + 端口映射。

### 3.2 裸金属 Linux Docker Host

同一条命令，compose 自动适配 bridge 网络。如果服务器有多个网卡需要指定 announce IP:

```bash
hostname -I                          # 查真实 IP
# 无需改 compose, Redis 容器会自动解析到 bridge IP
# 但若其他机器要访问此 Redis 集群, 在根目录 .env 加一行:
echo "REDIS_ANNOUNCE_IP=$(hostname -I | awk '{print $1}')" >> .env
# 然后把 compose 里动态解析逻辑改成直接用 REDIS_ANNOUNCE_IP 环境变量
```

## 四、集群验证

### 4.1 cluster-init 退出码 0 = 成功

```bash
docker compose logs redis-cluster-init --tail=20
# 预期看到:
# cluster_state:ok
# cluster_slots_assigned:16384
# cluster_known_nodes:6
# cluster_size:3
# Cluster ready.
```

`redis-cluster-init` 是 `restart: 'no'` 的一次性 Job，成功后退出，`docker compose ps` 显示 `Exited (0)` — 正常现象。

### 4.2 手动检查

```bash
docker compose exec -T redis-7000 sh -c 'REDISCLI_AUTH=$REDIS_PASSWORD redis-cli cluster info'
docker compose exec -T redis-7000 sh -c 'REDISCLI_AUTH=$REDIS_PASSWORD redis-cli cluster nodes'
```

预期 `cluster nodes` 输出 3 行 `connected`，每行 announce 一个 **bridge IP**（如 `172.20.0.2:7000@17000`）。这是设计如此 — 节点间 Gossip 必须用真实可达的容器 IP 才能互相发现。

### 4.3 Windows 本机 TCP 验证

```powershell
Test-NetConnection 127.0.0.1 -Port 7000  # TcpTestSucceeded : True
Test-NetConnection 127.0.0.1 -Port 3306
```

## 五、后端启动

### 5.1 配置来源

后端**不需要设置任何环境变量**。`application.yml` 里配置了：

```yaml
spring:
  config:
    import: optional:file:./.env[.properties]
```

Spring Boot 启动时会把根目录的 `.env` 当作 properties 文件加载，`application.yml` 中的 `${DB_URL}`、`${REDIS_PASSWORD}` 等占位符直接取用其中同名 key。

> 因此必须**从项目根目录**启动（`mvn spring-boot:run` / `java -jar` 的 working directory 为项目根），否则 `./.env` 相对路径找不到。

`.env` 中的 key 与 `application.yml` 的对应关系：

| .env key | application.yml 位置 |
|---|---|
| `SERVER_PORT` | `server.port` |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `spring.datasource` |
| `APP_REDIS_MODE` / `APP_REDIS_NODES` / `REDIS_PASSWORD` | `app.redis` |
| `REDIS_LOCK_WATCHDOG_MS` / `REDIS_LOCK_WAIT_SECONDS` | `app.redis.watchdog-timeout-millis` / `app.lock.wait-seconds` |
| `MYSQL_ROOT_PASSWORD` / `MYSQL_APP_PASSWORD` / `MYSQL_HOST_PORT` | Docker Compose 专用，Spring 不读取 |

系统环境变量仍然优先于 `.env`（Spring Boot 固定优先级），如需临时覆盖可直接 `set` 环境变量。

### 5.2 启动 Spring Boot

```bash
cd <项目根目录>
mvn.cmd spring-boot:run         # Windows
mvn spring-boot:run             # Linux
```

启动成功标志:

```
... HikariPool-1 - Start completed.
... Successfully applied 1 migration (Flyway V1__init.sql)
... 1 connections initialized for 127.0.0.1:7001
... Started FencingApplication in 2.652 seconds
```

**Redisson 连接逻辑**: Cluster-aware 客户端从 `127.0.0.1:7000` 拿到 cluster nodes 列表（节点 announce 的是 bridge IP），然后 NatMapper 把所有 bridge IP 重写回 `127.0.0.1:7000/7001/7002`，通过 Docker 端口映射 AUTH。

### 5.3 打包后运行

```bash
mvn.cmd clean package -q
java -jar target/redis-mysql-fencing-demo-0.0.1-SNAPSHOT.jar
```

> `java -jar` 同样要在项目根目录执行，才能读取 `./.env`；若部署到其他目录，用 `spring.config.import` 的绝对路径或直接注入环境变量。

## 六、前端启动

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
```

`vite.config.ts` 配置:

```typescript
server: { proxy: { '/api': 'http://localhost:8080' } }
```

页面有三个测试区:

1. **Basic ops** — GET snapshot / POST add (+delta)
2. **Concurrent stress** — N 个并发 POST，验证最终 value = N × delta（证明 fencing 阻止了丢失更新）
3. **Idempotency** — 同 Key 同 delta 重放返回 `replayed=true`，同 Key 不同 delta 返回 `409`

## 七、运维命令

```bash
# 查看所有 Redis 节点状态
docker compose ps
docker compose logs redis-7000 redis-7001 redis-7002 redis-7003 redis-7004 redis-7005 --tail=100

# 安全停机 (保留数据卷)
docker compose down

# 重启
docker compose up -d

# 查看数据卷
docker volume ls | grep redis-fencing-single-host

# 强制重建 (危险 — 删所有数据)
docker compose down -v
docker compose up -d
```

> **不要用 `down -v`** 除非你明确想重置 Redis 集群元数据和 MySQL Token。Fencing Token 不允许回退。

## 八、常见故障排查

### 8.1 `cluster_known_nodes: 1` — 节点只看到自己

**根因**: `cluster-announce-ip 127.0.0.1`。每个节点把 127.0.0.1 当成自己，Gossip 时互相 talk-to-self，永远只有 1 节点。

**处理**: compose 里用动态解析的 bridge IP（`getent hosts $(hostname)`），**不要写 127.0.0.1**。删 volume 重建:

```bash
docker compose down
docker volume rm \
  redis-fencing-single-host_redis7000_data \
  redis-fencing-single-host_redis7001_data \
  redis-fencing-single-host_redis7002_data \
  redis-fencing-single-host_redis7003_data \
  redis-fencing-single-host_redis7004_data \
  redis-fencing-single-host_redis7005_data
docker compose up -d
```

### 8.2 Redisson `AUTH timeout` / `Can't connect to servers`

**根因**: Cluster nodes 返回 bridge IP（如 172.20.0.5:7001），Redisson 尝试直接 AUTH 这些 IP，但 Windows 主机不在 Docker bridge 网络里。

**处理**: 在 `RedisConfiguration.java` 的 cluster 配置上 `.setNatMapper()`，把所有 bridge IP 重写为 `127.0.0.1`:

```java
.setNatMapper(uri -> new RedisURI(uri.getScheme(), "127.0.0.1", uri.getPort()))
```

### 8.3 MySQL `Access denied for user 'fencing_app'@'172.20.0.1'`

**根因**: 删过 MySQL volume 重建后，缓存的 JDBC 连接池里还有旧连接。或 .env 密码跟实际 MySQL 里存的不一致。

**处理**:

```bash
# 1. 确认密码一致
docker compose exec mysql sh -c 'echo $MYSQL_APP_PASSWORD'
# 对比根目录 .env 里的 MYSQL_APP_PASSWORD

# 2. 强制重建 MySQL volume
docker compose rm -sf mysql
docker volume rm redis-fencing-single-host_mysql_data
docker compose up -d mysql
```

### 8.4 前端 415 `Content-Type 'text/plain;charset=UTF-8' is not supported`

**根因**: fetch 请求 `headers: { 'Content-Type': 'application/json', ...init.headers }` 后展开 `...init` 把默认 headers 覆盖掉了。

**处理**: 确保 `...init` 在 headers **前面**展开:

```typescript
fetch(url, {
  ...init,                                      // 先展开
  headers: { 'Content-Type': 'application/json', ...init.headers },  // 再合并
})
```

### 8.5 MySQL `caching_sha2_password` 兼容

MySQL 8.4 默认用 `caching_sha2_password`。`mysql-connector-j 9.7` 支持，但需要 JDBC URL 里确保 `connectionTimeZone` 参数正确（已在 application.yml 里配好）。

### 8.6 集群初始化卡住

```bash
docker compose logs redis-cluster-init --tail=50
```

如果看到 `Waiting for the cluster to join` 超过 30 秒，检查 Redis 节点间 TCP 可达性:

```bash
docker compose exec redis-7000 sh -c "getent hosts redis-7001"
docker compose exec redis-7000 sh -c "REDISCLI_AUTH=$REDIS_PASSWORD redis-cli -h redis-7001 -p 7001 ping"
```

三节点都能互 PING 但 cluster 仍然卡，大概率是旧 `nodes.conf` 残留了错误的 announce IP。删 volume 重建。

重建后用 `redis-cli cluster nodes` 验证 6 行输出，其中 3 行带 `master` + slots 范围，3 行带 `slave` + `replicates <master_id>`。

## 九、限制与安全

| 项目 | 说明 |
|---|---|
| 拓扑 | 3 Master + 3 Replica（同机），Master 故障自动切 Replica；宿主机故障整体不可用 |
| 持久化 | Redis AOF + MySQL binlog 非零数据丢失保证 |
| 密码安全 | Redis/MySQL 用明文密码在 compose 环境变量，演示方案；生产换 ACL / Docker secrets |
| 网络隔离 | 端口映射到 0.0.0.0，本机 Docker Desktop 安全；部署到裸金属必须配合防火墙 |
| TLS | 无加密传输，仅限可信网络 |
| Redis Cluster Bus | 端口 17000-17005 仅节点间互访，不要暴露公网 |

生产建议:

- Master 和 Replica 分散到不同物理机（3 台宿主机 × 2 实例 = 3M + 3R 真正 HA）
- MySQL 主从 + MHA 或 Orchestrator 自动故障切换
- 所有密码走 Secret Manager
- Redis ACL 替代 `requirepass`
- 网络层 mTLS / 专用 VPC

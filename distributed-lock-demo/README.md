# Distributed Lock Demo

一个 Java 21 + Spring Boot Demo，用统一业务用例对比三种分布式锁：

- Redisson / Redis：`RLock` + WatchDog
- ZooKeeper：Apache Curator `InterProcessMutex`
- MySQL：唯一索引 + owner + expire_at + CAS 抢占过期锁

## 1. 架构

```text
HTTP Request
    |
    v
InventoryService
    |
    v
DistributedLockExecutor
    |
    +--> RedissonDistributedLock ----> Redis
    |
    +--> ZookeeperDistributedLock ---> ZooKeeper
    |
    +--> MysqlDistributedLock --------> MySQL

共享业务资源 demo_inventory 也存放于 MySQL。
```

业务 Demo 故意采用 `SELECT stock -> sleep -> UPDATE stock` 的非原子读改写过程。
不加锁时容易发生 lost update；加任意一种分布式锁后，同一个 `productId` 的临界区被串行化。

## 2. 环境

- JDK 21
- Maven 3.9+
- Docker + Docker Compose

## 3. 启动

```bash
docker compose up -d
mvn spring-boot:run
```

默认端口：

| Component | Port |
|---|---:|
| Spring Boot | 8080 |
| Redis | 6379 |
| ZooKeeper | 2181 |
| MySQL | 3306 |

MySQL：`root/root`，数据库：`distributed_lock_demo`。

## 4. API

### 重置库存

```bash
curl -X POST "http://127.0.0.1:8080/api/demo/inventory/reset?productId=1001&stock=20"
```

### 单次 Redisson 扣库存

```bash
curl -X POST "http://127.0.0.1:8080/api/demo/decrement/redisson?productId=1001"
```

provider 可选：

```text
redisson
zookeeper
mysql
```

### 并发基线：不加锁

```bash
curl -X POST "http://127.0.0.1:8080/api/demo/concurrent-unsafe?productId=1001&initialStock=20&requests=20"
```

由于多个线程可能同时读到相同库存值，通常会看到 `finalStock > 0`，这是 lost update。

### Redisson 并发测试

```bash
curl -X POST "http://127.0.0.1:8080/api/demo/concurrent/redisson?productId=1001&initialStock=20&requests=20"
```

### ZooKeeper 并发测试

```bash
curl -X POST "http://127.0.0.1:8080/api/demo/concurrent/zookeeper?productId=1001&initialStock=20&requests=20"
```

### MySQL 并发测试

```bash
curl -X POST "http://127.0.0.1:8080/api/demo/concurrent/mysql?productId=1001&initialStock=20&requests=20"
```

三种加锁测试正常情况下最终：

```json
{
  "finalStock": 0
}
```

Windows PowerShell 可直接运行：

```powershell
.\scripts\test-all.ps1
```

Linux/macOS：

```bash
./scripts/test-all.sh
```

## 5. 三种锁的实现重点

### Redisson

入口：`RedissonDistributedLock`

- Key：`distributed-lock-demo:inventory:{productId}`
- 使用 `RLock.tryLock(waitTime, unit)`
- 不手动设置固定 leaseTime，保留 WatchDog 自动续期
- 释放前通过 `isHeldByCurrentThread()` 验证当前线程确实持锁
- `RLock` 支持可重入

### ZooKeeper

入口：`ZookeeperDistributedLock`

- 使用 Curator `InterProcessMutex`
- 底层利用 ZooKeeper 的临时/顺序节点与 Watch 协调机制
- 会话失效后临时节点随之清理
- `InterProcessMutex` 支持可重入
- lock key 先做 Base64 URL-safe 编码，避免业务 key 中 `/` 破坏 ZNode 路径

### MySQL

入口：`MysqlDistributedLock`

表：`distributed_lock`

核心唯一约束：

```sql
UNIQUE KEY uk_distributed_lock_key(lock_key)
```

第一次抢锁：

```sql
INSERT INTO distributed_lock(lock_key, owner, expire_at) ...
```

唯一键冲突后尝试 CAS 抢占过期锁：

```sql
UPDATE distributed_lock
SET owner = ?, expire_at = ...
WHERE lock_key = ?
  AND expire_at < CURRENT_TIMESTAMP(6);
```

只有 `affected rows = 1` 才代表抢占成功。

释放锁：

```sql
DELETE FROM distributed_lock
WHERE lock_key = ?
  AND owner = ?;
```

`owner` 用于防止客户端 A 的旧请求误删客户端 B 后来获得的锁。

> Demo 的 MySQL 锁没有实现自动续租；因此临界区执行时间必须明显小于 leaseTime。生产场景若使用 MySQL 通用锁，需要增加续租/ fencing token 等保护，或者优先采用成熟协调组件。

## 6. 多实例验证

同一台机器可以启动两个 Spring Boot 实例：

```bash
SERVER_PORT=8080 mvn spring-boot:run
SERVER_PORT=8081 mvn spring-boot:run
```

两个实例连接同一 Redis、ZooKeeper、MySQL。
对相同 `productId` 同时发请求时，三种实现都能跨 JVM 竞争同一个分布式锁。

## 7. 为什么业务层还需要幂等

分布式锁解决的是“同一时间谁进入临界区”，不等于业务幂等。
生产订单/支付场景仍建议组合：

```text
分布式锁
+ 业务状态机
+ 幂等请求号
+ 数据库唯一约束
+ 本地事务
```

这样即使请求重试、锁过期、网络抖动，也有数据库层最终兜底。

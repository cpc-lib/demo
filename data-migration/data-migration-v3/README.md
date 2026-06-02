# 百万数据迁移生产级案例：Keyset 游标分片版

这是一个可运行的 Spring Boot 3 + MySQL 项目，用于演示 **同库百万级数据迁移/归档** 的生产级落地方式。

重点修复点：**不能假设 ID 是 1~1000000 连续递增**。真实生产表可能使用雪花 ID、大整数 ID、删除后断层 ID、历史归档断层 ID。本版本已改成 **Keyset/游标分片**，按真实存在的主键生成分片和批次。

核心流程：

```text
生成任务规划 -> 扫描真实主键生成分片 -> 创建临时表 -> 线程池并行迁移 -> 批次日志 -> 失败重试 -> count/checksum 校验 -> 校验通过后切换目标表
```

## 一、能力清单

- 同一个数据库 URL，同库表迁移：`user_source -> user_target`
- `migration_task`：迁移任务主表，记录 `id_column`
- `migration_plan_shard`：分片规划表
- `migration_batch_log`：批次执行日志表
- `migration_log`：操作日志表
- 临时表迁移：`user_target_tmp_xxx`
- 幂等写入：`ON DUPLICATE KEY UPDATE`
- 失败分片自动重试
- 失败分片单独重试
- 进度百分比、TPS 统计
- `COUNT + SUM(idColumn)` checksum 校验
- 支持真实生产 ID 不连续：雪花 ID、大起始 ID、删除断层、大量空洞
- Keyset/游标分片：按真实存在的主键切片，不按 `1~1000000` 假设
- 线程池大小、分片大小、批次大小可配置

## 二、为什么不能按 `1~1000000` 切？

错误做法：

```text
minId = 900000000000000000
maxId = 900000000010000000
shardSize = 50000
for start = minId; start <= maxId; start += shardSize
```

如果真实表只有 100 万行，但 ID 是雪花 ID 或者中间大量删除，这种算法会产生大量空区间：

```text
900000000000000000 ~ 900000000000049999：可能 0 行
900000000000050000 ~ 900000000000099999：可能 0 行
...
```

正确做法是：

```sql
SELECT id
FROM user_source
ORDER BY id;
```

然后应用层每读取 `shardSize` 个真实 ID，生成一个分片：

```text
分片1：第 1 个真实ID ~ 第 50000 个真实ID
分片2：第 50001 个真实ID ~ 第 100000 个真实ID
...
```

执行时再用游标推进：

```sql
SELECT id
FROM user_source
WHERE id > ? AND id <= ?
ORDER BY id
LIMIT ?;
```

这样不关心 ID 是否连续，只关心真实存在的数据行。

## 三、为什么这版比普通 demo 更可靠？

普通 demo 常见问题：

```text
1. 直接清空目标表，失败后数据半残
2. 无任务规划，失败后不知道从哪里恢复
3. 无分片记录，无法单独重试失败分片
4. 无批次日志，无法定位哪一批失败
5. 重复执行可能重复插入或主键冲突
6. 线程池随便开，容易打爆连接池
7. 按 minId/maxId 数值区间切片，ID 不连续时大量空扫
```

本项目解决方式：

```text
1. 先迁移到临时表，不污染正式目标表
2. 任务表记录全局状态
3. 分片表记录每个分片状态
4. 批次日志记录每个 batch 的范围、耗时、TPS、错误
5. 使用 ON DUPLICATE KEY UPDATE 保证重试幂等
6. 线程池参数受配置限制
7. 规划和执行都使用 Keyset 游标，避免空扫
8. 校验通过后才允许 switch
```

## 四、启动 MySQL

```bash
docker compose up -d
```

初始化脚本会创建：

- `user_source`：源表，自动生成 100 万数据，ID 故意设计成 `900000000000000000+` 的非连续大整数，模拟真实雪花 ID/断层 ID
- `user_target`：目标表
- `migration_task`
- `migration_plan_shard`
- `migration_batch_log`
- `migration_log`

## 五、启动项目

```bash
mvn clean package -DskipTests
java -jar target/million-data-migration-production-demo-1.0.0.jar
```

## 六、接口说明

### 1. 生成迁移规划

```bash
curl -X POST http://localhost:8080/api/migration/plan \
  -H "Content-Type: application/json" \
  -d '{
    "sourceTable": "user_source",
    "targetTable": "user_target",
    "idColumn": "id",
    "shardSize": 50000,
    "batchSize": 5000,
    "threadSize": 16
  }'
```

返回中的 `taskNo` 后续接口要用。

规划逻辑：

```text
1. 使用 SELECT id FROM source ORDER BY id 流式扫描真实存在的主键
2. 每 shardSize 个真实 ID 形成一个分片边界 start_id/end_id
3. 创建临时表 user_target_tmp_xxx
4. 写入 migration_task
5. 写入 migration_plan_shard
```

注意：这里不是 `min_id + shardSize` 的数值切片，而是 **真实 ID 游标切片**。即使 ID 从 `900000000000000000` 开始，或者中间删除了大量数据，也不会生成大量空分片。

### 2. 执行迁移

```bash
curl -X POST http://localhost:8080/api/migration/execute/MIG20260602120000000
```

执行逻辑：

```text
1. 查询 PLANNED / FAILED 分片
2. 创建线程池
3. 每个线程执行一个分片
4. 分片内部使用 keyset 游标：id > lastCursor ORDER BY id LIMIT batchSize
5. 每批执行 INSERT INTO tmp SELECT FROM source
6. 写入 migration_batch_log
7. 更新 migration_plan_shard 状态
8. 更新 migration_task 进度和 TPS
```

执行阶段同样不做 `batchStart += batchSize`，因此不会因为 ID 断层而空扫。

### 3. 重试失败分片

```bash
curl -X POST http://localhost:8080/api/migration/retry-failed/MIG20260602120000000
```

只会重试 `migration_plan_shard.status = FAILED` 的分片。

### 4. 校验

```bash
curl -X POST http://localhost:8080/api/migration/verify/MIG20260602120000000
```

校验内容：

```text
source count == tmp count
source sum(idColumn) == tmp sum(idColumn)
```

注意：`SUM(idColumn)` 是演示版 checksum。生产环境建议根据业务字段做 CRC32/MD5 分片校验。

### 5. 切换到正式目标表

```bash
curl -X POST http://localhost:8080/api/migration/switch/MIG20260602120000000
```

必须先 `verify` 通过，状态为 `VERIFIED` 才允许切换。

切换逻辑：

```text
1. TRUNCATE user_target
2. INSERT INTO user_target SELECT * FROM user_target_tmp_xxx
3. 记录 target checksum
4. 任务状态改为 SWITCHED
```

如果是生产真实大表，更推荐：

```text
1. 目标影子表 user_target_new
2. 校验通过
3. RENAME TABLE user_target TO user_target_old, user_target_new TO user_target
4. 保留 old 表用于回滚
```

本 demo 为了保持表结构简单，采用 `TRUNCATE + INSERT`。

### 6. 一键执行完整流程

```bash
curl -X POST http://localhost:8080/api/migration/plan-execute-verify-switch \
  -H "Content-Type: application/json" \
  -d '{
    "sourceTable": "user_source",
    "targetTable": "user_target",
    "idColumn": "id",
    "shardSize": 50000,
    "batchSize": 5000,
    "threadSize": 16
  }'
```

### 7. 查询任务

```bash
curl http://localhost:8080/api/migration/task/MIG20260602120000000
```

### 8. 查询分片

```bash
curl http://localhost:8080/api/migration/shards/MIG20260602120000000
```

### 9. 查询批次日志

```bash
curl http://localhost:8080/api/migration/batch-logs/MIG20260602120000000
```

### 10. 查询操作日志

```bash
curl http://localhost:8080/api/migration/logs/MIG20260602120000000
```

## 七、线程池规划建议

配置在 `application.yml`：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 40
migration:
  default-shard-size: 50000
  default-batch-size: 5000
  default-thread-size: 16
  max-thread-size: 32
```

建议：

```text
迁移线程数 <= Hikari maximum-pool-size - 预留连接数
```

例如：

```text
Hikari 最大连接数：40
预留业务/管理连接：8~12
迁移线程数：16~24 比较稳
```

## 八、百万数据 20 秒内的关键点

要接近或达到 20 秒，需要满足：

```text
1. 同库迁移优先用 INSERT INTO target SELECT FROM source
2. 不要 Java 逐行查出再逐行写入
3. 不要 offset 分页
4. 按真实存在的主键 Keyset 分片，不按数值区间死切
5. 每批 2000~10000 条
6. 临时表索引尽量少
7. MySQL buffer pool 不能太小
8. 磁盘 I/O 不能太弱
9. 连接池和线程池要匹配
```

本项目批次 SQL：

```sql
-- 执行前会先用：id > lastCursor ORDER BY id LIMIT batchSize 找到真实批次边界
INSERT INTO tmp(id,user_name,age,phone,email,create_time,update_time)
SELECT id,user_name,age,phone,email,create_time,update_time
FROM source
WHERE id BETWEEN ? AND ?
ON DUPLICATE KEY UPDATE
  user_name=VALUES(user_name),
  age=VALUES(age),
  phone=VALUES(phone),
  email=VALUES(email),
  update_time=VALUES(update_time);
```

## 九、限制说明

当前项目支持的是 **BIGINT 类型、可排序、有索引、最好唯一的迁移游标字段**，例如：

```text
id BIGINT PRIMARY KEY
snowflake_id BIGINT UNIQUE KEY
```

如果你的主键是 UUID/VARCHAR，建议下一版改成：

```text
migration_plan_shard.start_key VARCHAR(128)
migration_plan_shard.end_key VARCHAR(128)
migration_batch_log.start_key VARCHAR(128)
migration_batch_log.end_key VARCHAR(128)
```

执行 SQL 改成字符串 keyset：

```sql
WHERE uuid_col > ?
ORDER BY uuid_col
LIMIT ?
```

不过 UUID 随机分布会导致局部性差，迁移性能通常不如 BIGINT 主键。

## 十、生产环境还可以继续增强

如果是真实线上迁移，建议再增加：

```text
1. RENAME TABLE 原子切换
2. binlog 增量同步
3. 分片 CRC32/MD5 checksum
4. 限流，避免影响线上业务
5. 迁移任务互斥锁，禁止同一表同时迁移
6. 任务取消/暂停/继续
7. Prometheus 指标暴露
8. 迁移前自动检查表结构一致性
9. 自动检测 idColumn 是否存在索引
10. UUID/VARCHAR 游标字段支持
```

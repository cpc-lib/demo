# XXL-JOB Point Reward Platform

一个完整的分布式积分定时发放示例：Java 17 + Spring Boot 3.5.x + XXL-JOB 3.4.2 + Kafka + MySQL + React + Ant Design +
Zustand。

## 八层标准

1. XXL-JOB 调度策略：降低重复触发概率。
2. 单机串行：同一 Executor 内避免同 Job 并发。
3. 分片广播：`shardIndex/shardTotal` 拆分用户集合。
4. 确定性 `biz_no`：`DAILY_REWARD:userId:yyyyMMdd`。
5. MySQL `UNIQUE(biz_no)`：最终业务幂等屏障。
6. `@Transactional`：积分流水与账户余额原子提交/回滚。
7. `points = points + ?`：数据库原子累加，避免 lost update。
8. Kafka：削峰、解耦、至少一次投递；消费重试耗尽后进入 DLT。

## 故障语义

系统不试图保证 Exactly-Once Execution，而是采用 At-Least-Once + Idempotency 实现 Exactly-Once Business Effect。XXL-JOB
重试、Kafka 重复投递、Consumer 重启都可以重复执行，但相同 `biz_no` 的流水只能插入一次，因此积分只加一次。

## 一键启动

前提：Docker + Docker Compose，可访问 Docker Hub / GitHub Raw。

```bash
bash scripts/start.sh
```

入口：

- React 管理台：`http://localhost:3000`
- 后端 API：`http://localhost:8088/api/dashboard`
- XXL-JOB Admin：`http://localhost:8080`，默认账号 `admin / 123456`

首次启动会由 compose 下载 XXL-JOB 3.4.2 官方数据库脚本并初始化 `xxl_job` 数据库；本项目自己的积分业务表由
`infra/mysql/init/01-point-reward.sql` 初始化。

## XXL-JOB Job 配置

执行器 AppName：`point-reward-executor`

创建任务：

- JobHandler：`dailyPointRewardJob`
- 调度类型：CRON
- Cron：`0 0 1 * * ?`
- 路由策略：`分片广播 / SHARDING_BROADCAST`
- 阻塞策略：`单机串行 / SERIAL_EXECUTION`
- 失败重试：2~3

测试时可在 XXL-JOB 后台点击“执行一次”。

## 关键表

- `user_account`：积分账户。
- `user_point_ledger`：不可重复的积分账本，`UNIQUE(biz_no)`。
- `point_reward_batch`：发放批次。
- `point_reward_batch_shard`：每个 XXL-JOB 分片执行状态。
- `point_reward_failure`：Kafka DLT 失败记录和人工补偿状态。

## Kafka 重试和 DLT

`DefaultErrorHandler` 使用指数退避（1s → 2s → 4s，最大约 8s 时间窗）。仍失败的消息被发布至：

`point.reward.command.DLT`

DLT Consumer 将失败业务落入 `point_reward_failure`。React 的“失败补偿”页可重新投递原消息；原 `biz_no` 不变，所以补偿仍然保持幂等。

## 对账

“积分对账”页面比较：

- 活跃用户数
- 已发用户数
- 应发积分 = 活跃用户数 × 10
- 实际账本积分
- 缺失用户

如果用户覆盖与积分金额都一致，则显示平衡。

## 本地开发

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

前端 Vite 会将 `/api` 代理到 `localhost:8088`。本机运行后端时 Kafka 默认连接 `localhost:29092`；Docker 内部后端连接
`kafka:9092`。

## 幂等事务核心

事务执行顺序固定：

```text
BEGIN
  INSERT user_point_ledger (UNIQUE biz_no)
  UPDATE user_account SET points = points + ?
COMMIT
```

唯一键冲突由事务外层捕获为 `DUPLICATE`，不会继续更新积分。不能使用 `SELECT exists -> UPDATE -> INSERT` 的 check-then-act
模式。

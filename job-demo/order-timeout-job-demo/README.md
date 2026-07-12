# Spring Cloud Alibaba + XXL-JOB / PowerJob / ElasticJob 关闭超时未支付订单 Demo

## 1. 目标

用三个独立 Spring Boot 服务分别接入：

- `xxl-job-order-service`：XXL-JOB 执行器模式
- `powerjob-order-service`：PowerJob Worker 模式
- `elasticjob-order-service`：ElasticJob-Lite 去中心化调度模式

统一业务：扫描 `t_order` 表中 `status = UNPAID` 且 `pay_deadline < NOW()` 的订单，并更新为 `CLOSED`。

> 重点约束：cron 表达式不写在代码里，也不写在 `application.yml` 里。业务侧 cron 存放在 MySQL `job_cron_config` 表；XXL-JOB / PowerJob 平台侧 cron 应通过管理后台或平台数据库配置。

## 2. 技术栈

| 组件 | 版本建议 | 用途 |
|---|---:|---|
| JDK | 17 | 运行 Spring Boot 3 |
| Spring Boot | 3.2.12 | Web / 配置 / 事务 |
| MyBatis-Plus | 3.5.9 | 数据访问 |
| MySQL | 8.0.36 | 订单库、调度配置、任务日志 |
| Nacos | 2.3.2 | Spring Cloud Alibaba 环境基础服务 |
| XXL-JOB | 2.4.1 | 中心化任务调度 |
| PowerJob | 4.3.9 | 分布式任务调度 |
| ElasticJob | 3.0.4 | 去中心化分片任务调度 |
| ZooKeeper | 3.9 | ElasticJob 注册中心 |

## 3. 项目结构

```text
order-timeout-job-demo
├── docker-compose.yml                  # 虚拟机基础环境：MySQL / Nacos / ZooKeeper / XXL-JOB Admin / PowerJob Server
├── docker-compose.apps.yml             # 三个业务服务容器部署配置
├── mysql/init/01-schema.sql            # 订单表、cron配置表、任务日志表、测试数据
├── scripts
│   ├── start-env.sh
│   ├── stop-env.sh
│   └── build-all.sh
├── xxl-job-order-service
├── powerjob-order-service
└── elasticjob-order-service
```

## 4. 核心表设计

### 4.1 订单表

```sql
CREATE TABLE t_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(32) NOT NULL COMMENT 'UNPAID/PAID/CLOSED',
  pay_deadline DATETIME NOT NULL,
  close_reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  KEY idx_status_deadline (status, pay_deadline)
);
```

### 4.2 cron 配置表

```sql
CREATE TABLE job_cron_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_type VARCHAR(32) NOT NULL UNIQUE COMMENT 'XXL/POWER/ELASTIC',
  job_name VARCHAR(128) NOT NULL,
  cron_expr VARCHAR(64) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

默认数据：

```sql
INSERT INTO job_cron_config(job_type, job_name, cron_expr, enabled) VALUES
('XXL','xxlCloseTimeoutUnpaidOrder','0 */1 * * * ?',1),
('POWER','powerCloseTimeoutUnpaidOrder','0 */1 * * * ?',1),
('ELASTIC','elasticCloseTimeoutUnpaidOrder','0 */1 * * * ?',1);
```

### 4.3 任务执行日志表

```sql
CREATE TABLE order_close_job_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_type VARCHAR(32) NOT NULL,
  instance_id VARCHAR(128),
  batch_no VARCHAR(64) NOT NULL,
  scanned_count INT NOT NULL DEFAULT 0,
  closed_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  error_msg TEXT,
  started_at DATETIME NOT NULL,
  finished_at DATETIME
);
```

## 5. 业务关闭逻辑

核心原则：

1. 只关闭 `UNPAID` 订单。
2. 只关闭 `pay_deadline < NOW()` 的订单。
3. 使用数据库乐观锁：`WHERE id = ? AND status = 'UNPAID' AND version = ?`。
4. 每次执行写入 `order_close_job_log`。
5. 多实例并发执行时，重复扫描不会重复关闭订单。

核心 SQL：

```sql
SELECT *
FROM t_order
WHERE status='UNPAID'
  AND pay_deadline < NOW()
ORDER BY id ASC
LIMIT ?;

UPDATE t_order
SET status='CLOSED',
    close_reason=?,
    version=version+1,
    updated_at=NOW()
WHERE id=?
  AND status='UNPAID'
  AND version=?;
```

## 6. 三种调度实现差异

| 框架 | 调度模式 | cron 存放位置 | 分布式特点 | 本 Demo 实现方式 |
|---|---|---|---|---|
| XXL-JOB | Admin 中心化调度 | XXL-JOB Admin 数据库 / 后台页面 | Admin 触发执行器，执行器可多实例 | `@XxlJob("closeTimeoutUnpaidOrderHandler")` |
| PowerJob | Server 中心化调度 | PowerJob Server 数据库 / 后台页面 | Server 派发任务到 Worker，可 MapReduce / 广播 / 单机 | `BasicProcessor` |
| ElasticJob | 去中心化调度 | 本业务 MySQL `job_cron_config`，启动时注册到 ZooKeeper | ZooKeeper 协调分片、失效转移 | `SimpleJob + ScheduleJobBootstrap` |

## 7. 本地开发启动

### 7.1 启动基础环境

```bash
cd order-timeout-job-demo
bash scripts/start-env.sh
```

或：

```bash
docker compose up -d mysql nacos zookeeper xxl-job-admin powerjob-server
```

### 7.2 本地启动三个服务

```bash
mvn -pl xxl-job-order-service spring-boot:run
mvn -pl powerjob-order-service spring-boot:run
mvn -pl elasticjob-order-service spring-boot:run
```

服务地址：

| 服务 | 地址 |
|---|---|
| XXL 订单服务 | http://localhost:18081/orders |
| PowerJob 订单服务 | http://localhost:18082/orders |
| ElasticJob 订单服务 | http://localhost:18083/orders |
| XXL-JOB Admin | http://localhost:8080/xxl-job-admin |
| PowerJob Server | http://localhost:7700 |
| Nacos | http://localhost:8848/nacos |

## 8. 虚拟机部署

### 8.1 构建 jar

```bash
bash scripts/build-all.sh
```

### 8.2 启动基础环境

```bash
docker compose up -d mysql nacos zookeeper xxl-job-admin powerjob-server
```

### 8.3 启动业务服务

```bash
docker compose -f docker-compose.yml -f docker-compose.apps.yml up -d --build
```

### 8.4 查看日志

```bash
docker logs -f xxl-job-order-service
docker logs -f powerjob-order-service
docker logs -f elasticjob-order-service
```

## 9. XXL-JOB 配置方式

进入 XXL-JOB Admin：

```text
http://虚拟机IP:8080/xxl-job-admin
默认账号：admin
默认密码：123456
```

新增执行器：

| 配置项 | 值 |
|---|---|
| AppName | `xxl-order-executor` |
| 名称 | `订单超时关闭执行器` |
| 注册方式 | 自动注册 |

新增任务：

| 配置项 | 值 |
|---|---|
| 执行器 | `xxl-order-executor` |
| 任务描述 | `关闭超时未支付订单` |
| 路由策略 | 轮询 / 故障转移均可 |
| Cron | 从 MySQL `job_cron_config` 查询后填入，例如 `0 */1 * * * ?` |
| 运行模式 | BEAN |
| JobHandler | `closeTimeoutUnpaidOrderHandler` |

> 注意：XXL-JOB 本身的调度 cron 由 Admin 数据库存储，不在业务代码或 yml 中。业务服务内部还会读取 `job_cron_config` 判断是否启用。

## 10. PowerJob 配置方式

进入 PowerJob 控制台后创建应用：

| 配置项 | 值 |
|---|---|
| 应用名称 | `powerjob-order-worker` |

新增任务：

| 配置项 | 值 |
|---|---|
| 任务名称 | `关闭超时未支付订单` |
| 处理器类型 | Java Processor |
| Processor | `com.demo.order.job.PowerCloseTimeoutOrderProcessor` |
| Cron | 从 MySQL `job_cron_config` 查询后填入，例如 `0 */1 * * * ?` |

> 注意：PowerJob 的 cron 由 PowerJob Server 数据库存储，不在业务代码或 yml 中。

## 11. ElasticJob 配置方式

ElasticJob Demo 采用业务 MySQL 存储 cron：

```sql
SELECT cron_expr FROM job_cron_config WHERE job_type='ELASTIC';
```

服务启动时读取 MySQL cron，并通过 `ScheduleJobBootstrap` 注册到 ZooKeeper：

```java
JobConfiguration jobConfig = JobConfiguration
    .newBuilder("elasticCloseTimeoutUnpaidOrder", 1)
    .cron(cron)
    .overwrite(true)
    .build();
```

修改 cron：

```sql
UPDATE job_cron_config
SET cron_expr='0 */2 * * * ?', enabled=1
WHERE job_type='ELASTIC';
```

然后重启 `elasticjob-order-service`，新 cron 会重新注册。

## 12. 测试验证

### 12.1 查看订单

```bash
curl http://localhost:18081/orders
```

### 12.2 查看任务日志

```sql
SELECT * FROM order_close_job_log ORDER BY id DESC LIMIT 20;
```

### 12.3 查看订单是否关闭

```sql
SELECT order_no, status, pay_deadline, close_reason
FROM t_order
ORDER BY id;
```

预期：

- `NO_TIMEOUT_001`、`NO_TIMEOUT_002` 会被关闭。
- `NO_NORMAL_001` 未超时，不关闭。
- `NO_PAID_001` 已支付，不关闭。

## 13. 生产优化建议

1. XXL-JOB / PowerJob 的平台数据库应独立于业务库。
2. 订单关闭建议分页循环处理，避免单批数据过大。
3. 可增加 Redis 分布式锁，但不是必须；本 Demo 已通过数据库条件更新保证幂等。
4. 关闭订单后如果要释放库存，应发送 MQ 事件：`OrderClosedEvent`。
5. 大规模订单场景建议用 MQ 延迟消息优先，定时任务作为补偿扫描。
6. 任务执行日志建议接入 Prometheus / Grafana / ELK。
7. cron 修改后，XXL-JOB / PowerJob 推荐走管理后台；ElasticJob 可扩展为监听 MySQL 配置变更并动态 reschedule。

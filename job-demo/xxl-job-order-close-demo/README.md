# XXL-JOB 超时未支付订单关单示例

技术栈：Java 17、Spring Boot 4.0.0、XXL-JOB 3.3.1、MySQL 8.0.36、JdbcTemplate。

## 1. 核心设计

XXL-JOB 只负责触发任务，MySQL 条件更新保证业务幂等：

```sql
UPDATE biz_order
SET status = 2,
    close_time = NOW(3),
    close_reason = 'PAY_TIMEOUT',
    version = version + 1,
    updated_at = NOW(3)
WHERE id = ?
  AND status = 0
  AND expire_time <= NOW(3);
```

只有仍为未支付且已经过期的订单才能更新成功。任务重试、重复调度、人工重复执行、支付回调并发时，更新行数为 0 的请求不会重复关单。

## 2. Windows 一键启动

在项目根目录执行：

```powershell
docker compose config
docker compose up -d --build

# 或直接运行
.\start.ps1
```

查看状态：

```powershell
docker compose ps
docker compose logs -f order-executor
```

服务地址：

- XXL-JOB 管理端：http://localhost:8080/xxl-job-admin
- 默认账号：`admin`
- 默认密码：`123456`
- 订单查询：http://localhost:8081/api/orders
- 健康检查：http://localhost:8081/actuator/health

> MySQL 初始化脚本只会在数据卷首次创建时执行。若修改初始化 SQL，需要执行 `docker compose down -v` 后再启动；该命令会删除测试数据。

## 3. XXL-JOB 管理端配置

### 3.1 新增执行器

- AppName：`xxl-order-executor`
- 名称：`订单关单执行器`
- 注册方式：自动注册

等待执行器在线后，可在执行器列表看到注册地址。

### 3.2 启动任务

初始化 SQL 已自动创建任务：

- 任务描述：`关闭超时未支付订单`
- Cron：`0 0/1 * * * ? *`
- 运行模式：BEAN
- JobHandler：`closeTimeoutOrderJobHandler`
- 任务参数：`batchSize=200,maxRounds=20`
- 路由策略：第一个
- 阻塞处理策略：单机串行
- 任务超时时间：60 秒
- 失败重试次数：1

首次登录后进入“任务管理”，先手动执行验证，再点击“启动”。

## 4. 预期测试结果

初始化数据中，前 4 条订单是超时未支付。任务首次执行后：

```sql
SELECT order_no, status, close_time, close_reason, version
FROM order_demo.biz_order
ORDER BY id;
```

预期：

- `ORDER202607120001` 至 `ORDER202607120004` 变成 `status=2`
- `close_reason=PAY_TIMEOUT`
- `version` 从 0 增加到 1
- 未超时、已支付、已关闭订单不变

再次执行任务，关闭数量应为 0，验证幂等。

## 5. 本地 Java 启动

先仅启动 MySQL 和 XXL-JOB：

```powershell
docker compose up -d mysql xxl-job-admin
```

然后执行：

```powershell
mvn clean test
mvn spring-boot:run
```

本地执行器默认配置：

- Admin：`http://127.0.0.1:8080/xxl-job-admin`
- AppName：`xxl-order-executor`
- Executor Port：`9999`
- Web Port：`8081`

## 6. 并发边界

支付回调也必须采用条件更新，例如：

```sql
UPDATE biz_order
SET status = 1,
    pay_time = NOW(3),
    version = version + 1,
    updated_at = NOW(3)
WHERE order_no = ?
  AND status = 0;
```

支付和关单并发时，先成功更新状态的一方获胜。若第三方支付已成功但本地关单先完成，应进入支付对账与自动退款流程，不能只依赖定时任务解决跨系统最终一致性。

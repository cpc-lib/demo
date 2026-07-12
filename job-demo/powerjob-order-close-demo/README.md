# PowerJob 关闭超时未支付订单（Java / Windows / VMware）

本项目针对以下环境：

- Java 17、Maven 3.8+、Spring Boot 2.7.18
- PowerJob Worker 5.1.2
- Windows 本机开发和运行 Worker
- MySQL、PowerJob Server 运行在 VMware 虚拟机
- 虚拟机 IP：`192.168.220.200`

## 1. 幂等与并发设计

核心不是依赖 PowerJob “绝不重复执行”，而是让业务本身可重复执行：

```sql
UPDATE biz_order
SET status = 2,
    close_time = NOW(),
    version = version + 1
WHERE id = ?
  AND status = 0
  AND expire_time <= NOW();
```

只有 `affectedRows = 1` 才表示本次真正完成关闭。以下情况均不会重复关闭：

- PowerJob 失败重试；
- 人工重复触发；
- 两台 Worker 同时处理同一订单；
- 支付回调与关闭任务并发；
- Server 故障恢复后再次下发。

`order_close_log.order_id` 还有唯一索引，保证关闭审计记录只写一次。该方案不依赖 Redis 分布式锁，避免锁失效、续期和服务不可用问题。

## 2. 项目结构

```text
src/main/java/com/example/orderjob
├─ job          PowerJob Processor 与任务参数
├─ service      批处理服务、单订单事务服务
├─ repository   JDBC CAS 更新与分页扫描
├─ domain       状态和结果模型
└─ web          本地联调接口
```

## 3. PowerJob 与 MySQL 连通性

Windows PowerShell：

```powershell
Test-NetConnection 192.168.220.200 -Port 7700
Test-NetConnection 192.168.220.200 -Port 3306
```

两项都必须显示 `TcpTestSucceeded : True`。

数据库默认连接：

```text
jdbc:mysql://192.168.220.200:3306/order_demo
username=root
password=root123456
```

可通过环境变量修改：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`。

## 4. 初始化数据库

先在虚拟机 MySQL 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS order_demo
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

应用启动时会执行 `src/main/resources/db/schema.sql` 创建业务表。

## 5. PowerJob 控制台配置

浏览器打开：

```text
http://192.168.220.200:7700
```

### 5.1 创建应用

应用名称必须完全一致：

```text
order-close-worker
```

### 5.2 创建任务

建议配置：

| 配置项 | 值 |
|---|---|
| 任务名称 | close-expired-unpaid-orders |
| 执行类型 | 单机执行 / STANDALONE |
| Processor 类型 | Java 内建处理器 |
| Processor 信息 | expiredUnpaidOrderCloseProcessor |
| 定时策略 | CRON |
| CRON | `0 */1 * * * ?` |
| 任务参数 | `{"batchSize":200,"maxPages":50}` |
| 失败重试次数 | 3 |
| 最大同时运行实例数 | 1 |

不要把 CRON 写在 Java 或 `application.yml` 中；它由 PowerJob 控制台保存和管理。

## 6. Windows 启动

### 6.1 找到宿主机 VMware IP

```powershell
ipconfig
```

找到 `VMware Network Adapter VMnet8`，通常是：

```text
192.168.220.1
```

### 6.2 开放 Worker 端口

管理员 PowerShell：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\open-firewall.ps1
```

### 6.3 启动开发环境

```powershell
mvn clean test
.\scripts\start-dev.ps1 -WorkerIp 192.168.220.1
```

脚本会设置：

```text
PowerJob Server = 192.168.220.200:7700
Worker 地址     = 192.168.220.1:27777
MySQL           = 192.168.220.200:3306
```

注意：`powerjob.worker.server-address` 只解决 Worker 连接 Server；还必须使用 JVM 参数固定 Worker 上报地址：

```text
-Dpowerjob.network.local.address=192.168.220.1
```

否则 PowerJob 可能选中 Wi-Fi、VPN、Docker 或其他错误网卡 IP，导致任务无法下发。

## 7. 验证流程

### 7.1 创建一个已超时订单

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/api/orders/demo?expired=true&amount=99.90"
```

### 7.2 手工验证关闭逻辑

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/api/orders/internal/close-once?batchSize=200&maxPages=50"
```

### 7.3 查询订单

```powershell
Invoke-RestMethod "http://localhost:8080/api/orders"
```

订单状态：

- `UNPAID`：未支付
- `PAID`：已支付
- `CLOSED`：已关闭

### 7.4 验证幂等

连续多次调用关闭接口或在 PowerJob 控制台连续执行任务，订单仍只会关闭一次：

```sql
SELECT id, order_no, status, close_time, version
FROM biz_order
ORDER BY id DESC;

SELECT order_id, COUNT(*)
FROM order_close_log
GROUP BY order_id;
```

每个订单的关闭日志数量最多为 `1`。

## 8. 启动第二个 Worker 验证分布式调度

先打包：

```powershell
mvn clean package
```

窗口一：

```powershell
.\scripts\start-jar.ps1 -WorkerIp 192.168.220.1
```

窗口二：

```powershell
.\scripts\start-second-worker.ps1 -WorkerIp 192.168.220.1
```

两个 Worker 使用同一 `app-name=order-close-worker`，但端口分别为：

- Worker 1：`27777`
- Worker 2：`27778`

PowerJob 的 STANDALONE 模式会从可用 Worker 中选择一个执行。即使重复下发，数据库 CAS 仍保证幂等。

## 9. 任务仍无法调度时按顺序排查

1. Windows 到虚拟机：`Test-NetConnection 192.168.220.200 -Port 7700`；
2. Windows 到 MySQL：`Test-NetConnection 192.168.220.200 -Port 3306`；
3. 虚拟机到 Windows：测试 `192.168.220.1:27777`；
4. Worker 日志确认出现 `powerjob.network.local.address=192.168.220.1` 对应地址；
5. 控制台应用名必须是 `order-close-worker`；
6. Processor 信息必须是 `expiredUnpaidOrderCloseProcessor`；
7. Windows 防火墙开放 TCP `27777`（脚本使用 `Profile Any`，避免 VMware 网卡被识别为 Public 时仍被拦截）；
8. 多实例时 Worker 端口不能重复；
9. PowerJob Server 与 Worker 版本尽量保持一致。

更完整的网络说明见：`docs/VMWARE_NETWORK.md`。

## 10. 构建

```powershell
mvn clean test
mvn clean package -DskipTests
```

生成：

```text
target/powerjob-order-close-demo-1.0.0.jar
```

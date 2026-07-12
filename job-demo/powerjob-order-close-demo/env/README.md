# PowerJob 4.3.9 + MySQL 8.0.36（跨 Docker 网络修复版）

此版本用于以下部署结构：

```text
Windows 本地 Java Worker
        │
        │ 192.168.220.200:7700 / 10010
        ▼
VMware Linux 虚拟机
        └─ Docker bridge
             ├─ PowerJob Server 4.3.9
             └─ MySQL 8.0.36
```

## 本次修复

旧配置下，PowerJob Server 会把 Docker 容器地址（例如 `192.168.224.3:10010`）返回给 Windows Worker，导致：

```text
ConnectTimeoutException: connection timed out: /192.168.224.3:10010
```

新配置通过以下 JVM 参数公布虚拟机对外可访问地址：

```text
-Dpowerjob.network.external.address=192.168.220.200
-Dpowerjob.network.external.port.http=10010
-Dpowerjob.network.external.port.akka=10086
```

不要把 `powerjob.network.local.address` 设置为虚拟机宿主地址；Docker bridge 容器通常无法绑定该地址。

## 目录结构

```text
powerjob-mysql-4.3.9-fixed/
├─ .env
├─ docker-compose.yml
├─ worker-application-example.yml
├─ mysql/init/01-powerjob-schema.sql
└─ scripts/
   ├─ start.ps1
   ├─ stop.ps1
   ├─ reset.ps1
   ├─ check-ports.ps1
   ├─ start.sh
   ├─ stop.sh
   └─ reset.sh
```

## 启动

在 VMware Linux 虚拟机中解压并进入目录：

```bash
docker compose down --remove-orphans
docker compose up -d --force-recreate
docker compose ps
docker compose logs -f powerjob-server
```

或者：

```bash
bash scripts/start.sh
```

PowerJob 控制台：

```text
http://192.168.220.200:7700
```

数据库默认配置：

```text
地址：192.168.220.200:3306
数据库：powerjob
用户：powerjob
密码：PowerJob123456
root 密码：root123456
```

正式环境请修改 `.env` 中的密码。

## Windows Worker 配置

复制 `worker-application-example.yml` 中的配置：

```yaml
powerjob:
  worker:
    enabled: true
    app-name: order-service
    password: "123456"
    server-address: 192.168.220.200:7700
    protocol: http
    port: 27777
    allow-lazy-connect-server: false
```

Worker 与 Server 的 PowerJob 依赖版本建议统一为 `4.3.9`。

## 检查端口

在 Windows PowerShell 执行：

```powershell
Test-NetConnection 192.168.220.200 -Port 7700
Test-NetConnection 192.168.220.200 -Port 10010
Test-NetConnection 192.168.220.200 -Port 10086
```

或执行：

```powershell
.\scripts\check-ports.ps1
```

在 VMware Linux 中执行：

```bash
sudo ss -lntp | grep -E '3306|7700|10010|10086'
docker port powerjob-server
docker compose logs --tail=300 powerjob-server
```

Ubuntu 防火墙：

```bash
sudo ufw allow 3306/tcp
sudo ufw allow 7700/tcp
sudo ufw allow 10010/tcp
sudo ufw allow 10086/tcp
sudo ufw reload
```

Windows 防火墙还需要允许 Java Worker 使用的 `27777/TCP` 入站连接，否则 Worker 能注册但任务可能无法下发。

## 验证修复

Worker 日志中不应再出现：

```text
192.168.224.3:10010
```

应当连接到：

```text
192.168.220.200:10010
```

## 修改虚拟机 IP

如果虚拟机地址发生变化，只修改 `.env`：

```dotenv
POWERJOB_EXTERNAL_ADDRESS=新的虚拟机IP
```

然后重新创建 Server：

```bash
docker compose up -d --force-recreate powerjob-server
```

## 数据库首次初始化

初始化 SQL 只会在 MySQL 数据卷首次创建时执行。若仍然出现：

```text
Unknown database 'powerjob'
```

并且确认没有需要保留的数据，可执行：

```bash
docker compose down -v --remove-orphans
docker compose up -d
```

警告：`down -v` 会删除现有 MySQL 和 PowerJob 数据。

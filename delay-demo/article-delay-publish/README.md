# Article Delay Publish

基于 **Java 21 + Spring Boot + Redis ZSet + Kafka + MySQL + React 管理端** 的可靠文章延时发布工程。

## 技术栈

- Java 21
- Spring Boot 3.5.16
- Spring Data JPA
- Redis 8（ZSet + Lua）
- Apache Kafka 4.2.1（KRaft）
- MySQL 8.4
- Flyway
- Spring Kafka Retry Topic + DLT
- Actuator / Micrometer
- Kafbat Kafka UI
- React 18 + TypeScript + Ant Design + Axios + Vite

## 关键设计

```text
                 schedule / reschedule / cancel
Client ───────────────> Spring Boot ───────────────> MySQL
                              │                       Source of Truth
                              │ after commit
                              v
                       Redis scheduled ZSet
                              │
                         Lua atomic claim
                              v
                       Redis processing ZSet
                              │
                         Kafka producer
                              v
                            Kafka
                              │
                       retry topics / DLT
                              v
                         Kafka consumer
                              │
                  conditional idempotent UPDATE
                              v
                            MySQL
```

这不是“扫描到就 `ZREM` 再发 Kafka”的脆弱实现。任务先进入带租约的 `processing` ZSet：

- Kafka ACK：确认删除。
- Kafka send 失败：重新入 `scheduled`。
- 应用在 ACK 前宕机：租约到期自动回收。
- DB 已提交但 Redis 没写成功：MySQL 补偿扫描自动修复。
- 重复 Kafka 消息：`status + schedule_version + publish_time` 条件更新保证幂等。

详细设计见：`docs/spec/article-delay-publish-spec.md`。

## 目录

```text
article-delay-publish/
├── docs/
│   ├── architecture.md
│   └── spec/article-delay-publish-spec.md
├── admin-ui/                 # React 管理端
│   ├── src/
│   ├── Dockerfile
│   └── nginx.conf
├── scripts/
│   ├── start-infra.ps1
│   ├── start-infra.sh
│   └── smoke-test.ps1
├── src/main/java/com/example/articledelay/
│   ├── api/
│   ├── application/
│   ├── config/
│   ├── domain/
│   ├── infrastructure/kafka/
│   ├── infrastructure/redis/
│   └── scheduler/
├── src/main/resources/
│   ├── db/migration/V1__create_article.sql
│   └── application.yml
├── src/test/
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## 方式一：本地运行 Spring Boot

要求：JDK 21、Maven 3.9+、Docker Desktop / Docker Engine。

### 1. 启基础设施

Windows PowerShell：

```powershell
./scripts/start-infra.ps1
```

或：

```powershell
docker compose up -d mysql redis kafka kafka-ui
```

### 2. 运行测试

```powershell
mvn clean test
```

### 3. 启动应用

```powershell
mvn spring-boot:run
```

### 4. 一键冒烟测试

另开一个 PowerShell：

```powershell
./scripts/smoke-test.ps1
```

成功时输出：

```text
SMOKE TEST PASSED
```

## 方式二：完全使用 Docker Compose

本机不需要安装 Maven：

```powershell
docker compose --profile app up -d --build
```

然后执行：

```powershell
./scripts/smoke-test.ps1
```

## 地址

| 服务 | 地址 |
|---|---|
| React 管理端 | `http://localhost:3000` |
| Spring Boot | `http://localhost:8080` |
| Health | `http://localhost:8080/actuator/health` |
| Kafka UI | `http://localhost:8090` |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |

数据库：

```text
database: article_delay
username: article
password: article
```

## React 管理端

Docker Compose 全量启动后：

```text
http://localhost:3000
```

单独本地开发：

```powershell
cd admin-ui
npm install
npm run dev
```

开发地址：`http://localhost:5173`。Vite 会把 `/api` 代理到 `http://localhost:8080`。

管理端功能：

- 文章分页列表
- 标题搜索 / 状态过滤
- 创建草稿
- 设置定时发布
- 修改发布时间
- 取消定时发布
- 查看文章详情
- 后端 `ProblemDetail` 错误展示

## API

### 分页查询文章

```http
GET /api/articles?page=0&size=10&keyword=Kafka&status=SCHEDULED
```

参数：

- `page`：从 `0` 开始。
- `size`：`1-100`。
- `keyword`：可选，按标题模糊搜索。
- `status`：可选，`DRAFT / SCHEDULED / PUBLISHED`。

### 创建草稿

```http
POST /api/articles
Content-Type: application/json

{
  "title": "Redis + Kafka 延时发布",
  "content": "正文"
}
```

### 定时发布

`publishAt` 必须携带时区偏移，服务内部统一转成 UTC `Instant`。

```http
POST /api/articles/1/schedule
Content-Type: application/json

{
  "publishAt": "2026-08-28T20:00:00+08:00"
}
```

再次调用同一接口即可修改发布时间；`schedule_version` 会自动递增，使旧 Redis/Kafka 任务失效。

### 取消定时

```http
POST /api/articles/1/cancel-schedule
```

### 查询文章

```http
GET /api/articles/1
```

## Redis Key

```text
article:delay:scheduled
article:delay:processing
```

member：

```text
articleId:scheduleVersion:publishAtEpochMillis
```

例如：

```text
10001:3:1787918400000
```

## Kafka

主 Topic：

```text
article.publish
```

消费者失败采用 Spring Kafka 非阻塞重试，最终进入 DLT。Kafka UI 可以直接观察主 topic、retry topic 与 DLT。

## 正确性说明

系统实现的是：

```text
at-least-once delivery + idempotent business update
```

不依赖跨 Redis / Kafka / MySQL 的分布式 exactly-once。即使发生重复投递，数据库只有满足以下条件的消息才能真正发布文章：

```text
id = articleId
status = SCHEDULED
schedule_version = event.scheduleVersion
publish_time <= now
```

因此：修改发布时间、取消定时、Kafka 重复消费、Redis lease 回收导致的重复发送，都不会造成错误发布。

## 清理环境

保留数据：

```powershell
docker compose down
```

连数据卷一起删除：

```powershell
docker compose down -v
```

# user-platform-demo (ES 7.x + MySQL Sharding Demo + Redis + Mongo Outbox + Kafka)

目标：
- **登录查询 200ms 内**（本地/同机容器环境）
- MySQL 分库分表（本项目用 **同库多表**模拟：`user_base_00..03`、`user_phone_index_00..03`、`user_email_index_00..03`）
- Mongo 作为 Outbox 本地消息表（可靠投递）
- Kafka 做消息总线
- ES 7.x 做后台用户检索：手机号/邮箱/昵称模糊 + 性别筛选 + **search_after** 分页（避免深翻页 from/size）

## 1. 启动环境（Docker Compose）
```bash
cd deploy
docker compose up -d
```

等待 ES / Kafka 就绪（首次启动可能 1~2 分钟）。

## 2. 初始化数据
MySQL 会自动执行 `deploy/mysql/init.sql`，创建分表并插入演示用户：
- phone: `13800000000`  password: `Passw0rd!`
- email: `demo@ivera.cc` password: `Passw0rd!`

## 3. 启动应用
```bash
mvn -q -DskipTests spring-boot:run
```

应用端口：`8080`

## 4. 测试接口

### 4.1 登录（200ms 内核心链路）
```bash
curl -X POST "http://localhost:8080/auth/login"   -H "Content-Type: application/json"   -d '{"tenantId":"t1","identifier":"13800000000","password":"Passw0rd!"}'
```

或者邮箱：
```bash
curl -X POST "http://localhost:8080/auth/login"   -H "Content-Type: application/json"   -d '{"tenantId":"t1","identifier":"demo@ivera.cc","password":"Passw0rd!"}'
```

### 4.2 创建/更新用户（写 MySQL + Mongo Outbox -> Kafka -> ES）
```bash
curl -X POST "http://localhost:8080/users" -H "Content-Type: application/json" -d '{
  "tenantId":"t1","phone":"13900000001","email":"u1@ivera.cc","nickname":"张三","gender":"M","password":"Passw0rd!"
}'
```

### 4.3 后台搜索（ES）
- 模糊：phone/email/nickname
- 过滤：gender/status/deleted
- 分页：`nextToken`（search_after）

```bash
curl -X POST "http://localhost:8080/admin/users/search" -H "Content-Type: application/json" -d '{
  "tenantId":"t1",
  "q":"138000",
  "gender":"F",
  "pageSize":10
}'
```

返回里有 `nextToken`，继续翻页传回即可。

## 5. 关键性能点（你要的 200ms）
- 登录链路优先走 **Redis：identifier->uid** & **uid->auth snapshot**，大多数请求 1~2 次 Redis 命中完成。
- Redis miss 才落 MySQL：索引表精确查（phone/email），再按 uid 路由 user_base 分表查 auth 字段。
- MySQL 连接池 + 索引覆盖（本 demo 已建索引）。
- ES 分页使用 `search_after`，避免 deep pagination。

## 6. 组件地址
- MySQL: `localhost:3306` (root / root123)
- Redis: `localhost:6379`
- Mongo: `localhost:27017`
- Kafka: `localhost:9092`
- ES: `localhost:9200`
- Kibana: `localhost:5601`


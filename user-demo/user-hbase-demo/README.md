# hbase-user-10b-demo (HBase 2.5 + ES 7)

一套“10亿用户”落地 Demo：
- **HBase**：用户主表（逻辑删除不物理删）
- **HBase 唯一索引表**：手机号/邮箱当前占用（注销后释放，可重新注册复用，生成新 userId）
- **Kafka**：注册/修改/注销事件统一上总线
- **MongoDB**：消费幂等表（eventId 去重）+ 重试控制 + 人工处理表
- **Elasticsearch 7**：后台检索（分页/模糊） + `external_gte` 外部版本防乱序覆盖
- **失败超过3次**：写入人工处理表，人工触发从 HBase 拉权威数据补写 ES

> HBase 2.5 Docker 镜像示例使用 `dqops/apache-hbase-standalone:2.5.4`（Docker Hub 可见）。citeturn0search1  
> 你也可以替换为公司内网 HBase 集群。

---

## 1. 一键启动依赖（Docker Compose）

```bash
docker-compose up -d
```

### 端口
- MongoDB: 27017
- Kafka: 9092
- Redis: 6379
- ES: 9200
- HBase UI: 16010
- HBase ZK(容器内映射到宿主机): 2182

> 注意：本 compose 里同时启动了 `zookeeper`(2181) 给 Kafka 用；HBase 容器自带 ZK 映射到 2182，避免冲突。

---

## 2. 启动应用

```bash
mvn -q -DskipTests package
java -jar target/hbase-user-10b-demo-1.0.0.jar
```

默认配置见 `application.yml`，可用环境变量覆盖：

- `HBASE_ZK_QUORUM` 默认 `localhost`
- `HBASE_ZK_PORT` 默认 `2181`（如果你用本 compose 里的 HBase，请改成 `2182`）
- `ES_HOSTS` 默认 `localhost:9200`
- `KAFKA_BOOTSTRAP` 默认 `localhost:9092`
- `MONGODB_URI` 默认 `mongodb://localhost:27017/user_demo`

示例（用本 compose）：
```bash
export HBASE_ZK_PORT=2182
java -jar target/hbase-user-10b-demo-1.0.0.jar
```

---

## 3. 表/索引初始化

应用启动会自动初始化：
- HBase：`u:user`、`u:uniq_phone`、`u:uniq_email`（带 00..ff 预分区）
- ES：index `user_profile`（mapping 包含 keyword/text + 基础字段）

如需关闭初始化：
- `APP_INIT_HBASE=false`
- `APP_INIT_ES=false`

---

## 4. API 说明

### 4.1 注册（phone 或 email 至少一个）
`POST /api/auth/register`
```json
{
  "phone": "13800000000",
  "email": "a@b.com",
  "password": "Passw0rd!"
}
```

### 4.2 登录（phone 或 email）
`POST /api/auth/login`
```json
{ "account": "13800000000", "password": "Passw0rd!" }
```

### 4.3 修改（phone/email 变更也要唯一）
`POST /api/user/{uid}/update`
```json
{ "phone": "13900000000", "email": "new@b.com" }
```

### 4.4 注销（HBase 逻辑删除 + 释放唯一索引，允许复用重新注册生成新 uid）
`POST /api/user/{uid}/cancel`

### 4.5 后台搜索（ES 分页）
`GET /admin/users/search?q=foo&page=1&size=10`

### 4.6 人工处理任务
- `GET /admin/manual-tasks?page=1&size=20`
- `POST /admin/manual-tasks/{eventId}/fix` （从 HBase 拉最新数据补写 ES）

---

## 5. 关键一致性规则（你要的全部在这里）

- **注册/变更唯一**：HBase `checkAndMutate(ifNotExists)` 原子占位。
- **注销可复用**：主表仅逻辑删除；并 `checkAndMutate(ifEquals(uid)) delete` 释放 uniq 表行。
- **重新注册**：一定生成新 `userId`，占位成功即可。
- **ES 不被乱序覆盖**：写入 ES 使用 `external_gte` + `version`（来自 HBase `cf:version`）。
- **消费幂等**：Mongo `_id=eventId` 去重；失败重试超过3次写 `manual_task`，人工补写 ES。


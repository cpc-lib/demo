# AI Vocabulary Studio Enterprise

基于 **Spring Boot 3 + Java 21 + LangChain4j + React + Ant Design** 的 AI 英文词卡学习系统。

## 核心能力

- Elasticsearch 关键词检索适配层。
- Milvus Standalone + Attu 管理面板已接入，语义搜索默认走 Milvus Adapter。
- Prompt Template 管理中心。
- SM-2 inspired 复习计划算法。
- Testcontainers / ArchUnit / GitHub Actions CI。
- Docker Compose 增加 Elasticsearch / Kibana / Milvus / Attu。

## 已有能力

- JWT 登录注册。
- AI 生成英文释义、中文含义、俚语、例句。
- 前端卡片预览、编辑、保存。
- PNG 导出。
- Redis 缓存。
- RabbitMQ 异步 Anki 导出。
- MinIO 文件存储。
- AI Token 用量日志。

## 启动

### 1. 启动基础环境

```bash
docker compose -f ai-vocab-card-env/docker-compose.yml up --build
```

`ai-vocab-card-env/` 目录集中存放本地运行环境：

- `docker-compose.yml`：MySQL、Redis、RabbitMQ、MinIO、Elasticsearch、Kibana、Milvus、Attu、Milvus Adapter 编排。
- `schema.sql`：MySQL 初始化脚本。
- `milvus-adapter/`：Milvus REST Adapter 构建上下文。

当前 `backend` 和 `frontend` 在 Compose 文件中保持注释，默认不会由 Compose 启动。

基础服务访问：

- RabbitMQ：http://localhost:15672
- MinIO Console：http://localhost:9001
- Elasticsearch：http://localhost:9200
- Kibana：http://localhost:5601
- Milvus gRPC：http://localhost:19530
- Milvus health：http://localhost:9091/healthz
- Attu Milvus 管理面板：http://localhost:3000
- Milvus Adapter：http://localhost:19531

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端访问：http://localhost:8080

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端访问：http://localhost:5173

默认账号可自行注册。没有配置 LLM Key 时，系统使用本地 fallback 结果，保证链路可运行。

## 配置真实大模型

```bash
export LLM_ENABLED=true
export LLM_API_KEY=你的key
export LLM_BASE_URL=https://api.openai.com/v1
export LLM_MODEL=gpt-4o-mini
```

兼容 DeepSeek / 通义千问 OpenAI Compatible API。

## API

```http
POST /api/words/generate
POST /api/words
GET  /api/words/search
GET  /api/words/{id}
GET  /api/search/keyword
POST /api/search/semantic
GET  /api/prompts
POST /api/prompts
DELETE /api/prompts/{id}
POST /api/export/anki
```

## 测试

```bash
cd backend
mvn clean verify
```

包含：

- JUnit5
- ArchUnit
- Testcontainers MySQL smoke test
- JaCoCo 覆盖率报告

## 面试项目描述

> 基于 Spring Boot + LangChain4j 构建 AI 英文词卡平台，支持大模型生成词卡、用户编辑保存、中文/英文关键词检索、语义搜索、个人词库、SM-2 复习计划、PNG 与 Anki 导出。系统采用 MySQL 存储主数据，Redis 做热点缓存，RabbitMQ 处理异步导出，MinIO 存储导出文件，Elasticsearch 承载全文检索，Prompt Template 支持模板管理。通过 ArchUnit、Testcontainers、GitHub Actions 提升工程质量。

## 当前边界

- Milvus 已通过独立 REST Adapter 接入，业务层不直接依赖 Milvus Java SDK，便于后续替换为官方 SDK 或其它向量库。
- Milvus Adapter 当前使用确定性本地向量生成器，生产环境建议替换为与 LangChain4j `EmbeddingModel` 一致的真实 embedding 模型。
- ES 默认未开启中文 IK 分词器，生产建议使用自定义 ES 镜像安装 IK。
- 搜索索引同步当前为保存后同步尝试，生产建议升级为 Outbox + MQ 最终一致性。


## 生产增强能力

生产增强包含：

- Milvus 向量搜索适配边界：`VectorSearchProvider`
- Milvus REST Adapter 默认启用，本地语义搜索仍可通过 `VECTOR_PROVIDER=local` 降级
- Outbox 事件表，解决 DB 与 MQ 最终一致性
- Redis 限流：`@RateLimited`
- Redis 幂等：`@Idempotent` + `Idempotency-Key`
- Anki 导出任务列表与失败重试接口
- Spring Boot Actuator 健康检查与指标端点
- 运维能力接口：`GET /api/ops/capabilities`

### 推荐启动

```bash
docker compose -f ai-vocab-card-env/docker-compose.yml up --build
```

### 关键接口

```http
GET /api/ops/capabilities
GET /api/ops/outbox?status=PENDING
GET /api/export/tasks?userId=1
POST /api/export/tasks/{id}/retry
```

### 幂等保存示例

```bash
curl -X POST http://localhost:8080/api/words \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: save-awesome-001' \
  -d '{"word":"awesome","englishDefinition":"Extremely good.","chineseMeaning":"极好的"}'
```

### Milvus 接入说明

默认启用 Milvus：

```bash
VECTOR_PROVIDER=milvus-adapter
MILVUS_ENABLED=true
MILVUS_ENDPOINT=http://milvus-adapter:19531
MILVUS_COLLECTION=word_card_vectors
MILVUS_DIMENSION=1024
```

除了 MySQL、Redis、RabbitMQ、MinIO、Elasticsearch、Kibana 等基础服务，Docker Compose 还会启动以下 Milvus 相关服务：

- `etcd`：Milvus 元数据依赖
- `minio`：Milvus 对象存储依赖，复用项目已有 MinIO
- `milvus`：Milvus Standalone，端口 `19530` / `9091`
- `attu`：Milvus 管理面板，端口 `3000`
- `milvus-adapter`：项目向量检索 REST 适配器，端口 `19531`

语义查询链路：

```text
Spring Boot Semantic Search
  -> MilvusVectorSearchProvider
  -> milvus-adapter /vectors/search
  -> Milvus Standalone
```

业务层不依赖具体向量库 SDK，便于切换 Milvus、pgvector、Qdrant 或 Elasticsearch dense_vector。

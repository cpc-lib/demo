# LangChain4j + Milvus Agent RAG Demo

这个版本是在原项目基础上做的“高价 AI 应用工程师版”改写，核心目标：

- 保留原有接口：`/api/chat`、`/api/chat/stream`、知识库导入、Milvus 动态切换接口都保留
- 新增 Agent Tool 能力
- 支持多轮对话（通过 `conversationId`）
- 优先查知识库，未命中再查互联网 Top10
- 自动追加标准化“数据来源”
- 增加天气查询 Tool，例如：`明天上海天气如何`
- `pom.xml` 可直接导入 IDEA

---

## 1. 技术设计

### 1.1 Agent 工具列表

- `knowledgeSearch`
  - 查询 Milvus 知识库
  - 用于内部文档、项目知识、上传文件问答
  - **优先调用**
- `webSearch`
  - 知识库没命中或信息不足时调用
  - 默认走 Tavily Search API
  - 返回前 10 条结果，并在最终回答里追加标准来源
- `weatherForecast`
  - 查询指定城市天气
  - 默认使用 Open-Meteo（免 API Key）

### 1.2 对话机制

- 通过 `conversationId` 维持多轮上下文
- 使用 `MessageWindowChatMemory`
- 不传 `conversationId` 时默认按单轮请求处理

### 1.3 回答策略

Agent 系统提示词已经内置以下规则：

1. 业务/技术/项目知识问题先查知识库
2. 知识库无结果或不足时再查互联网
3. 天气问题直接调天气工具
4. 最终回答后自动追加：
   - `【数据来源】`
   - `【工具调用轨迹】`

---

## 2. 配置说明

配置文件：`src/main/resources/application-prod.yml`

推荐通过环境变量注入：

```bash
export RAG_LLM_API_KEY=你的大模型Key
export RAG_EMBED_API_KEY=你的Embedding Key
export TAVILY_API_KEY=你的Tavily Key
export MILVUS_HOST=127.0.0.1
export REDIS_HOST=127.0.0.1
```

说明：

- 天气工具使用 Open-Meteo，无需额外 API Key
- 联网搜索默认使用 Tavily；如果不配置 `TAVILY_API_KEY`，则只能走知识库和天气工具

---

## 3. 启动依赖

```bash
docker compose -f env/docker-compose.yml up -d
```

如你的 `docker-compose.yml` 里已经包含 Milvus / Redis，直接启动即可。

---

## 4. 运行项目

```bash
mvn clean spring-boot:run
```

或者在 IDEA 中直接运行 `RagDemoApplication`。

---

## 5. 接口说明

## 5.1 导入文本到知识库

```bash
curl -X POST "http://localhost:8080/api/ingest/text" \
  -H "Content-Type: application/json" \
  -d '{"text":"Java线程池的核心参数包括 corePoolSize、maximumPoolSize、keepAliveTime、workQueue 等。"}'
```

## 5.2 导入文件到知识库

```bash
curl -X POST "http://localhost:8080/api/ingest/file" -F "file=@./docs/sample.md"
```

## 5.3 单轮对话

```bash
curl "http://localhost:8080/api/chat?question=Java线程池有哪些核心参数？"
```

## 5.4 多轮对话

第一轮：

```bash
curl "http://localhost:8080/api/chat?conversationId=demo-001&question=请先记住，我现在在学习 Java 并发。"
```

第二轮：

```bash
curl "http://localhost:8080/api/chat?conversationId=demo-001&question=那你继续讲一下线程池参数之间的关系。"
```

## 5.5 返回结构化详情（推荐调试）

```bash
curl "http://localhost:8080/api/chat/detail?conversationId=demo-001&question=Spring AOP 的底层原理是什么？"
```

会返回：

- answer
- 是否命中知识库
- 是否使用互联网搜索
- 是否使用天气工具
- sources
- toolTraces

## 5.6 SSE 流式输出

```bash
curl -N "http://localhost:8080/api/chat/stream?conversationId=demo-002&question=请解释一下 JVM Full GC 的常见原因"
```

> 说明：该版本为了兼容 Tool 调用链，SSE 采用“服务端切片推送最终答案”的方式，而不是底层模型 token 直推。接口保留不变。

---

## 6. 使用效果示例

### 6.1 知识库优先

```bash
curl "http://localhost:8080/api/chat?question=Java线程池有哪些核心参数？"
```

如果知识库已有内容，回答末尾会附上类似：

```text
【数据来源】
1. [KNOWLEDGE_BASE] sample.md | chunk=0 | 摘要=...

【工具调用轨迹】
- knowledgeSearch：命中知识库片段数=1
```

### 6.2 知识库没命中，自动联网

```bash
curl "http://localhost:8080/api/chat?question=OpenAI 最新发布了什么 Agent 相关能力？"
```

如果知识库没有命中，会自动调用 `webSearch`，并在答案后输出 Top10 来源。

### 6.3 天气 Tool

```bash
curl "http://localhost:8080/api/chat?question=明天上海天气如何？"
```

会调用 `weatherForecast` 工具，返回：

- 日期
- 天气现象
- 最高/最低温
- 降水概率
- 最大风速
- Open-Meteo 来源

---

## 7. 关键改造点

### 已保留

- `/api/chat`
- `/api/chat/stream`
- `/api/ingest/text`
- `/api/ingest/file`
- `/api/vector-stores/**`
- `/api/milvus/collections/**`

### 新增

- `AgentAssistant`：基于 LangChain4j AiServices 的 Agent 接口
- `KnowledgeTool`：知识库查询 Tool
- `WebSearchTool`：互联网检索 Tool
- `WeatherTool`：天气查询 Tool
- `AnswerRenderService`：统一拼装“答案 + 来源 + 工具轨迹”
- `/api/chat/detail`：结构化调试接口

---

## 8. 官方资料参考

本项目改造依赖的外部能力与文档：

- LangChain4j AI Services / Tools：官方文档与 GitHub 教程说明支持 `AiServices.builder(...).tools(...)`，并支持 `@MemoryId` 多轮对话、`Result<T>` 返回额外元数据。
- Tavily Search API：官方文档说明 `POST https://api.tavily.com/search`，支持 `Authorization: Bearer ...` 认证与 `max_results` 等参数。
- Open-Meteo：官方文档说明 Geocoding API 可解析城市坐标，Forecast API 可获取每日天气预报，且无需 API Key。

---

## 9. 建议你下一步继续增强

你如果要把它继续升成真正企业级版本，建议再加：

- Tool 路由器（先分类，再决定是否挂载全部 Tool）
- Persistent Chat Memory（Redis/MySQL 持久化）
- Web 搜索结果入库（二次沉淀到知识库）
- Re-ranker 重排
- Source citation JSON 化输出
- 单元测试 / 集成测试 / Dockerfile


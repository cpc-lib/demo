# LangChain4j Milvus RAG Demo v2

## 项目概述

本项目是一个基于 Spring Boot 3.2.6 的 RAG（Retrieval-Augmented Generation）演示应用，集成了 LangChain4j、Milvus 向量数据库、MySQL 关系数据库、Redis 缓存、RabbitMQ 消息队列等技术组件，提供完整的文档检索增强生成能力。

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.2.6 |
| ORM | MyBatis-Plus | 3.5.15 |
| AI SDK | LangChain4j | 0.35.0 |
| 向量数据库 | Milvus | - |
| 关系数据库 | MySQL | 8.x |
| 缓存 | Redis | 7.x |
| 消息队列 | RabbitMQ | - |
| 对象存储 | MinIO / Local | - |
| 前端 | React + TypeScript + Vite + Ant Design | - |

---

## 项目结构

### 后端结构 (`src/main/java/cc/ivera/ragdemo`)

```
src/main/java/cc/ivera/ragdemo/
├── RagDemoApplication.java          # Spring Boot 启动类
├── admin/                           # 租户删除管理
│   ├── TenantDeletionExecutor.java  # 租户删除执行器
│   ├── TenantDeletionWorker.java    # 删除工作器抽象
│   ├── MysqlTenantDeletionWorker.java
│   ├── MilvusTenantDeletionWorker.java
│   ├── ElasticsearchTenantDeletionWorker.java
│   ├── RedisTenantDeletionWorker.java
│   ├── ObjectStorageTenantDeletionWorker.java
│   └── ...
├── agent/                           # AI Agent
│   └── AgentAssistant.java          # Agent 助手
├── annotation/                      # 自定义注解
│   └── RateLimit.java               # 限流注解
├── audit/                           # 审计服务
│   └── TenantAuditService.java
├── config/                          # Spring 配置
│   ├── AiConfig.java                # AI 模型配置
│   ├── RagProperties.java           # RAG 配置属性
│   ├── RabbitIngestionConfig.java   # RabbitMQ 配置
│   ├── SchedulerConfig.java         # 定时任务线程池配置
│   ├── SecurityConfig.java          # 安全配置
│   ├── WebMvcConfig.java            # Web MVC 配置
│   └── ...
├── controller/                      # REST API 控制器
│   ├── RagController.java           # RAG 主控制器
│   ├── RagQueryController.java      # 查询控制器
│   ├── RagDocumentController.java   # 文档控制器
│   ├── KnowledgeChunkController.java# 块管理控制器
│   ├── ModelConfigController.java   # 模型配置控制器
│   ├── TenantIdentityController.java# 租户身份控制器
│   └── ...
├── domain/                          # 领域模型
│   ├── dto/                         # 数据传输对象
│   ├── pojo/                        # 简单对象
│   ├── rag/                         # RAG 实体
│   │   ├── RagDocument.java         # 文档实体
│   │   ├── RagDocumentChunk.java    # 文档块实体
│   │   ├── RagKnowledgeBase.java    # 知识库实体
│   │   ├── RagIngestionTask.java    # 摄入任务实体
│   │   ├── RagQueryLog.java         # 查询日志实体
│   │   ├── RagQueryFeedback.java    # 查询反馈实体
│   │   └── ...
│   ├── tenant/                      # 租户实体
│   │   ├── SysTenant.java           # 租户实体
│   │   ├── SysUser.java             # 用户实体
│   │   ├── RagTenantModelConfig.java# 租户模型配置
│   │   ├── RagTenantQuota.java      # 租户配额
│   │   └── ...
│   └── vo/                          # 视图对象
├── exception/                       # 异常处理
│   ├── GlobalExceptionHandler.java  # 全局异常处理器
│   ├── BizException.java            # 业务异常
│   └── ApiErrorCode.java            # API 错误码
├── mapper/                          # MyBatis-Plus Mapper
│   ├── RagDocumentMapper.java
│   ├── RagDocumentChunkMapper.java
│   ├── RagTenantModelConfigMapper.java
│   └── ...
├── model/                           # 请求/响应模型
│   ├── query/                       # 查询请求模型
│   │   ├── RagQueryRequest.java
│   │   ├── RagSearchRequest.java
│   │   ├── ChatRequest.java
│   │   └── ...
│   ├── knowledge/                   # 知识库模型
│   │   ├── KnowledgeIngestionResult.java
│   │   ├── KnowledgeChunkRecord.java
│   │   └── ...
│   ├── dto/                         # 数据传输对象
│   └── ChatAnswer.java              # 聊天回答模型
├── permission/                      # 权限服务
│   └── KnowledgeBasePermissionService.java
├── quota/                           # 配额服务
│   └── TenantQuotaService.java
├── service/                         # 业务服务
│   ├── RagChatService.java          # 聊天服务
│   ├── RagQueryService.java         # 查询服务
│   ├── KnowledgeIngestionService.java # 知识摄入服务
│   ├── KnowledgeChunkManagementService.java # 块管理服务
│   ├── RagDocumentIngestionService.java # 文档摄入服务
│   ├── RagIngestionExecutor.java    # 摄入执行器
│   ├── RagIngestionTaskConsumer.java # 消息消费者
│   ├── MetricsService.java          # 指标服务
│   ├── AnswerRenderService.java     # 回答渲染服务
│   ├── ingest/                      # 摄入子服务
│   │   ├── MultimodalDocumentParser.java # 多模态文档解析
│   │   ├── TikaParser.java          # Tika 解析器
│   │   ├── Splitter.java            # 文本分割器
│   │   ├── OcrTextExtractor.java    # OCR 提取器
│   │   └── VisionEmbeddingClient.java # 视觉嵌入客户端
│   ├── query/                       # 查询子服务
│   │   ├── RagRetrievalService.java # 检索服务
│   │   ├── RagReranker.java         # 重排序服务
│   │   ├── ElasticsearchKeywordSearchIndex.java # 关键词索引
│   │   ├── QueryCostAnalyticsService.java # 成本分析
│   │   ├── RagQueryLogService.java  # 查询日志服务
│   │   ├── RagRetrievalEvaluationService.java # 检索评估
│   │   └── ...
│   ├── rag/                         # RAG 核心服务
│   │   ├── PromptBuilder.java       # 提示词构建器
│   │   └── Retriever.java           # 检索器
│   ├── ragops/                      # RAG 操作工具
│   │   ├── RagHashing.java          # 哈希工具
│   │   ├── DocumentTextDiff.java    # 文档差异
│   │   ├── LocalObjectStorageService.java # 本地对象存储
│   │   ├── MinioObjectStorageService.java # MinIO 对象存储
│   │   ├── IngestionTaskStateMachine.java # 状态机
│   │   └── ...
│   ├── ratelimit/                   # 限流服务
│   │   └── SlidingWindowRateLimiter.java
│   ├── tenant/                      # 租户服务
│   │   ├── ModelConfigService.java  # 模型配置服务
│   │   └── DynamicModelFactory.java # 动态模型工厂
│   ├── tool/                        # Agent 工具
│   │   ├── KnowledgeTool.java       # 知识库工具
│   │   ├── WebSearchTool.java       # 网页搜索工具
│   │   ├── WeatherTool.java         # 天气工具
│   │   ├── TextToImageTool.java     # 图文工具
│   │   └── ...
│   ├── trace/                       # 链路追踪
│   │   └── AgentTraceContext.java
│   └── vector/                      # 向量存储服务
│       ├── DynamicMilvusStoreManager.java # 动态 Milvus 管理
│       ├── MultimodalVectorStore.java # 多模态向量存储
│       └── ActiveMilvusContext.java # 活跃 Milvus 上下文
└── util/                            # 工具类
    ├── LogMasker.java               # 日志脱敏工具
    └── PollingExecutor.java         # 非阻塞轮询工具
```

### 前端结构 (`ui/`)

```
ui/
├── src/
│   ├── api/                         # API 接口
│   │   ├── http.ts                  # HTTP 客户端
│   │   └── rag.ts                   # RAG API
│   ├── components/                  # 通用组件
│   │   └── PageHeaderCard.tsx
│   ├── layouts/                     # 布局组件
│   │   └── AppLayout.tsx
│   ├── pages/                       # 页面
│   │   ├── ChatPage.tsx             # 聊天页面
│   │   ├── KnowledgePage.tsx        # 知识库页面
│   │   ├── ChunksPage.tsx           # 块管理页面
│   │   ├── QueryLogsPage.tsx        # 查询日志页面
│   │   ├── ImageAssetsPage.tsx      # 图片资源页面
│   │   ├── CollectionsPage.tsx      # 集合页面
│   │   ├── VectorStoresPage.tsx     # 向量存储页面
│   │   ├── RetrievalEvaluationsPage.tsx # 检索评估页面
│   │   └── TenantAccessPage.tsx     # 租户访问页面
│   ├── types/                       # TypeScript 类型定义
│   │   └── index.ts
│   ├── utils/                       # 工具函数
│   │   └── message.ts
│   ├── App.tsx                      # 根组件
│   ├── main.tsx                     # 入口文件
│   └── styles.css                   # 全局样式
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── .env.example
```

---

## 核心功能

### 1. RAG 查询与检索

- **语义检索**: 基于 Milvus 向量数据库的相似度搜索
- **关键词检索**: 基于 Elasticsearch 的关键词搜索
- **混合检索**: 结合向量检索和关键词检索的融合策略
- **重排序**: 使用 Rerank 模型对检索结果进行重排序

### 2. 文档摄入

- **异步摄入**: 通过 RabbitMQ 实现文档摄入异步化
- **多格式支持**: PDF、DOCX、Markdown、TXT 等格式
- **多模态支持**: 图片提取、OCR、视觉分析
- **文本分割**: 智能文本切分与块管理

### 3. 块管理

- **版本控制**: 块的版本管理与回滚
- **状态管理**: 启用/禁用块状态
- **Redis 注册**: 块版本注册与一致性保证

### 4. 租户管理

- **多租户支持**: 基于租户的隔离
- **模型配置**: 每个租户独立的模型配置
- **配额管理**: 租户资源配额限制
- **数据清理**: 租户删除时的全链路数据清理

### 5. Agent 工具

- **知识库查询**: 查询内部知识库
- **网页搜索**: 使用 Tavily 进行网页搜索
- **天气查询**: 使用 OpenMeteo 查询天气
- **图片生成**: 使用 OpenAI 生成图片

### 6. 监控与分析

- **查询日志**: 完整的查询日志记录
- **反馈收集**: 用户反馈收集与分析
- **成本分析**: API 调用成本统计与异常检测
- **检索评估**: 检索效果评估与趋势分析

---

## API 接口

### 聊天接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat` | 聊天接口 |
| GET | `/api/chat/detail` | 聊天详情 |
| GET | `/api/chat/stream` | 流式聊天 |

### RAG 查询接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/rag/query` | RAG 查询 |
| POST | `/api/rag/search` | 向量搜索 |

### 文档摄入接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/rag/documents/ingest` | 文档摄入 |
| POST | `/api/rag/knowledge-bases/{id}/documents/ingest` | 知识库文档摄入 |
| GET | `/api/rag/documents/{id}` | 获取文档 |

### 块管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/rag/chunks` | 查询块列表 |
| GET | `/api/rag/chunks/{id}` | 获取块详情 |
| POST | `/api/rag/chunks` | 创建块 |
| PUT | `/api/rag/chunks/{id}` | 更新块 |
| DELETE | `/api/rag/chunks/{id}` | 删除块 |
| POST | `/api/rag/chunks/{id}/rollback` | 回滚版本 |
| POST | `/api/rag/chunks/{id}/disable` | 禁用块 |

### 模型配置接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/model-configs` | 获取模型配置列表 |
| GET | `/api/admin/model-configs/{id}` | 获取模型配置 |
| POST | `/api/admin/model-configs` | 新增模型配置 |
| PUT | `/api/admin/model-configs/{id}` | 更新模型配置 |
| DELETE | `/api/admin/model-configs/{id}` | 删除模型配置 |
| GET | `/api/admin/model-configs/cache-stats` | 获取缓存统计 |
| POST | `/api/admin/model-configs/cache-stats/reset` | 重置缓存统计 |

### 知识库接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/rag/knowledge-bases` | 获取知识库列表 |
| GET | `/api/rag/knowledge-bases/{id}` | 获取知识库详情 |
| POST | `/api/rag/knowledge-bases` | 创建知识库 |

---

## 关键配置

### 环境变量

| 变量名 | 说明 |
|--------|------|
| `RAG_LLM_BASE_URL` | LLM 模型基础 URL |
| `RAG_LLM_API_KEY` | LLM 模型 API Key |
| `RAG_EMBED_BASE_URL` | Embedding 模型基础 URL |
| `RAG_EMBED_API_KEY` | Embedding 模型 API Key |
| `MILVUS_HOST` | Milvus 主机地址 |
| `REDIS_HOST` | Redis 主机地址 |
| `RABBITMQ_HOST` | RabbitMQ 主机地址 |
| `RAG_OBJECT_DIR` | 对象存储目录 |
| `RAG_MULTIMODAL_INGEST_ENABLED` | 是否启用多模态摄入 |
| `RAG_VISION_ANALYSIS_ENABLED` | 是否启用视觉分析 |

### 配置文件

- `src/main/resources/application.yml` - 主配置文件
- `src/main/resources/application-prod.yml` - 生产环境配置
- `.mvn/maven.config` - Maven 配置

---

## 启动方式

### 后端启动

```bash
# 设置 JDK 版本
export JAVA_HOME=/path/to/jdk21
export PATH=$JAVA_HOME/bin:$PATH

# 开发环境运行
mvn spring-boot:run

# 打包
mvn clean package

# 运行打包后的 Jar
java -jar target/langchain4j-milvus-rag-demo-v2-1.1.0.jar
```

### 前端启动

```bash
cd ui
npm install
npm run dev
```

### 基础设施启动

```bash
docker compose -f rag-env/docker-compose.yml up -d
```

---

## 编码规范

- **Java 版本**: 21
- **编码格式**: UTF-8 (无 BOM)
- **构建工具**: Maven
- **ORM**: MyBatis-Plus
- **代码风格**: Lombok 注解优先
- **日志框架**: SLF4J + Spring Boot 默认实现

---

## 项目状态

- ✅ 代码编译通过
- ✅ 前端构建通过
- ✅ 所有文件编码统一为 UTF-8
- ✅ 测试代码已移除
- ✅ 硬编码问题已修复

# 004 V3 Enterprise 改造规格

## 状态
implemented

## 目标
在 V2.5 基础上补齐企业级运行能力：登录鉴权、Redis 缓存、RabbitMQ 异步导出、MinIO 文件存储、AI Token 用量统计、Docker Compose 一键启动，以及架构测试。

## 功能验收
- 用户可注册、登录，后续接口通过 JWT 访问。
- 词卡详情和搜索结果写入 Redis 缓存，保存词卡后清理详情缓存。
- AI 生成通过 LangChain4j OpenAI-compatible API，失败时使用本地可编辑降级结果。
- 每次 AI 调用写入 `ai_usage_log`，记录模型、用户、状态和估算 token。
- Anki 支持同步 TSV 下载，也支持 RabbitMQ 异步任务生成并上传 MinIO。
- Docker Compose 启动 MySQL、Redis、RabbitMQ、MinIO、Backend、Frontend。
- ArchUnit 禁止 Controller 直接访问 Mapper。

## 边界
- Elasticsearch / Milvus 在 V3 中以接口预留和文档说明为主，默认 MySQL + Redis 降级可运行。
- 未配置 LLM API Key 时，不阻塞主流程，使用 fallback 词卡，保证本地演示可用。

# 005 V4 Enterprise Search / Semantic / Prompt Spec

状态：implemented

## 1. 背景

V3 已完成 JWT、Redis、RabbitMQ、MinIO、Anki 异步导出与 AI Token 日志。V4 目标是补齐企业级检索、Prompt 管理、复习算法和工程质量能力。

## 2. 范围

- Elasticsearch 关键词搜索适配层。
- 本地语义搜索降级实现，后续可替换 Milvus。
- Prompt Template 管理接口。
- SM-2 inspired 复习计划。
- Testcontainers / ArchUnit / GitHub Actions。

## 3. API

### 关键词搜索

```http
GET /api/search/keyword?keyword=很棒&page=1&size=10
```

### 语义搜索

```http
POST /api/search/semantic
Content-Type: application/json

{
  "query": "表达很厉害的英文",
  "topK": 10
}
```

### Prompt 管理

```http
GET /api/prompts?code=WORD_CARD_GENERATE
POST /api/prompts
DELETE /api/prompts/{id}
```

## 4. 验收标准

- [x] 保存词卡后写入 MySQL 主表。
- [x] 保存词卡后生成本地语义检索记录。
- [x] 支持 `/api/search/keyword` 查询。
- [x] 支持 `/api/search/semantic` 查询。
- [x] Prompt 可入库管理，AI 生成优先使用数据库启用模板。
- [x] 默认无 LLM API Key 时仍可使用 fallback 结果跑通链路。
- [x] Docker Compose 包含 Elasticsearch / Kibana。
- [x] CI 包含后端测试、前端构建。

## 5. 后续可扩展

- 将 `local-keyword-jaccard` 替换为 LangChain4j EmbeddingModel + Milvus。
- 将 Prompt 管理升级为版本灰度、审批发布、回滚。
- 为 ES 配置 IK 中文分词器。

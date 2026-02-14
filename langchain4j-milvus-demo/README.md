# rag-qwen-tavily-milvus

Spring Boot 3.2 + LangChain4j RAG demo:
- Chat model: DashScope Qwen-Plus (OpenAI Compatible Mode)
- Vector DB: Milvus (docker-compose)
- Document parsing: Apache Tika (LangChain4j document parser)
- Web search: Tavily (as Tool / Function Call)
- Chat API: synchronous (no SSE)

## 1) Start Milvus

```bash
cd rag-qwen-tavily-milvus
docker compose up -d
```

Milvus ports:
- gRPC: `19530`
- HTTP: `9091`
Minio console: `http://localhost:9001` (minioadmin / minioadmin)

## 2) Configure keys

Edit `src/main/resources/application.yml`:

- `app.dashscope.api-key`
- `app.tavily.api-key`

## 3) Run

```bash
mvn -q -DskipTests spring-boot:run
```

## 4) Upload docs

```bash
curl -F "file=@./your.pdf" http://localhost:8080/api/files/upload
```

## 5) Chat

```bash
curl -H "Content-Type: application/json" \
  -d '{"question":"这份文档主要讲了什么？"}' \
  http://localhost:8080/api/chat
```

### Routing strategy

The assistant follows a strict tool-usage policy:
1) `knowledge_search` (Milvus RAG)
2) If empty/low relevance -> `tavily_search` (web)
3) If question is about weather -> `weather` (mock)

You can adjust `app.milvus.min-score` and `app.milvus.max-results` in `application.yml`.

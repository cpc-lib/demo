# Spring Boot RAG Demo (Tika → Splitter → Embeddings → Milvus → Retriever → Prompt → LLM → SSE)

## Start Redis + Milvus

```bash
docker compose up -d
```

## Run

```bash
mvn spring-boot:run
```

## Redis-based dynamic Milvus switching

This demo stores Milvus connection profiles in Redis and reads the current active profile at runtime.

### Redis keys

- `rag:milvus:active` → active Milvus alias
- `rag:milvus:aliases` → all configured aliases
- `rag:milvus:config:{alias}` → host / port / collection / topK / minScore

### APIs

Create or update a Milvus profile:

```bash
curl -X POST "http://localhost:8080/api/vector-stores" \
  -H "Content-Type: application/json" \
  -d '{
    "alias":"knowledge-a",
    "host":"127.0.0.1",
    "port":19530,
    "collection":"demo_kb_a",
    "topK":6,
    "minScore":0.55
  }'
```

Switch active Milvus:

```bash
curl -X POST "http://localhost:8080/api/vector-stores/switch?alias=knowledge-a"
```

View active Milvus:

```bash
curl "http://localhost:8080/api/vector-stores/current"
```

List all Milvus profiles:

```bash
curl "http://localhost:8080/api/vector-stores"
```

## Ingest text

```bash
curl -X POST "http://localhost:8080/api/ingest/text" \
  -H "Content-Type: application/json" \
  -d '{"text":"Java线程池的核心参数包括 corePoolSize、maximumPoolSize、keepAliveTime、workQueue 等。"}'
```

## Ingest file

```bash
curl -X POST "http://localhost:8080/api/ingest/file" -F "file=@./docs/sample.md"
```

## Ask with SSE streaming

```bash
curl -N "http://localhost:8080/api/chat/stream?question=Java线程池有哪些核心参数？"
```

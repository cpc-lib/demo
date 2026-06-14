# Spring Boot RAG Demo (Tika → Splitter → Embeddings → Milvus → Retriever → Prompt → LLM → SSE)

## Start Milvus

```bash
docker compose up -d
```

## Configure API key

```bash
export OPENAI_API_KEY="YOUR_KEY"
```

## Run

```bash
mvn spring-boot:run
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

Troubleshooting: if Milvus dimension mismatch, adjust `rag.embedding.dimension` in `application.yml`.

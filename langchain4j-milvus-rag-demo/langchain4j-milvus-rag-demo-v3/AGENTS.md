# AGENTS.md

This file gives coding agents the project-specific context needed to work safely in this repository.

## Project Summary

This is a Spring Boot RAG demo using:

- Java 21, Spring Boot 3.2.6, MyBatis-Plus 3.5.15
- LangChain4j 0.35.0 for chat, tools, embeddings, and Milvus retrieval
- Milvus for vector search
- MySQL for business metadata and RAG management tables
- Redis for runtime state, active Milvus config, and chunk version registry
- RabbitMQ for async RAG ingestion tasks
- Local filesystem object storage adapter, with MinIO intended as the production replacement point
- React 18 + TypeScript + Vite + Ant Design in `ui/`

The Git root is above this directory (`D:/code/demo`). Treat this project directory as the active application root.

## Important Architecture

Backend package root:

```text
src/main/java/cc/ivera/ragdemo
```

Important areas:

- `config/`: Spring configuration, AI model beans, RabbitMQ configuration, `RagProperties`, `SchedulerConfig`, security config.
- `controller/`: REST APIs for chat, ingestion, chunk management, vector store management, Milvus collection inspection, RAG metadata, model config.
- `domain/rag/`: MySQL entities for RAG metadata MVP.
- `domain/tenant/`: MySQL entities for tenant management, model config, quota, usage.
- `mapper/`: MyBatis-Plus mappers.
- `model/knowledge/`: ingestion, chunk, multimodal, and task DTOs/records.
- `model/query/`: request/response DTOs for RAG queries, feedback, evaluation.
- `service/`: business services.
- `service/ingest/`: document parsing and multimodal extraction (Tika, OCR, vision).
- `service/rag/`: retrieval and prompt-building.
- `service/ragops/`: small deterministic RAG operation utilities such as hashing, state machine, object storage adapter, policies.
- `service/vector/`: dynamic Milvus store switching, multimodal vector store.
- `service/tool/`: LangChain4j tools exposed to the agent (knowledge, web search, weather, image).
- `service/tenant/`: tenant model configuration services, `DynamicModelFactory`.
- `service/query/`: query logging, feedback workflow, cost analytics, retrieval evaluation.
- `admin/`: tenant deletion execution and workers.
- `util/`: Utility classes (`LogMasker`, `PollingExecutor`).

Frontend:

```text
ui/
```

SQL:

- Consolidated SQL bootstrap: `src/main/resources/sql/all-in-one.sql`
- This single script includes the previous work-order table, RAG metadata baseline, and all RAG upgrade sections in dependency order.

Docs:

- `CODE_INTRO.md`: Comprehensive project introduction document.
- `docs/spec-implementation-plan.md`: SDD mapping from `spec.md` to the current MVP.
- `docs/chunk-management.md`: chunk version-management API and behavior.
- `docs/plan/optimization-plan-v2.md`: Optimization plan document.

## RAG Storage Split

Follow the storage boundary from `spec.md`:

- MySQL stores auditable business state: knowledge base, document, chunk metadata, task status, vector ID mapping.
- Milvus stores vectors and searchable metadata payloads.
- Redis stores hot/runtime state and the existing chunk version registry.
- RabbitMQ carries async ingestion task messages.
- Local filesystem object storage currently stores uploaded original files. Replace `LocalObjectStorageService` when MinIO integration is implemented.

Do not store high-dimensional vectors in MySQL. Store vector IDs and metadata there.

## Ingestion Flow

Preferred async flow:

1. Upload file through `/api/rag/documents/ingest` or `/api/rag/knowledge-bases/{knowledgeBaseId}/documents/ingest`.
2. `RagDocumentIngestionService` stores the original file through `LocalObjectStorageService`.
3. It inserts `rag_document` and `rag_ingestion_task`.
4. It publishes `RagIngestionTaskMessage` to RabbitMQ.
5. `RagIngestionTaskConsumer` calls `RagIngestionExecutor`.
6. `RagIngestionExecutor` reads the object, delegates parsing/vector writing to `KnowledgeIngestionService`, and writes `rag_document_chunk` rows with `vector_id`.
7. It updates document and task statuses.

Public ingestion APIs are unified under RAG metadata ingestion:

- `POST /api/rag/documents/ingest` for multipart file uploads or JSON text ingestion.
- `POST /api/rag/knowledge-bases/{knowledgeBaseId}/documents/ingest` for multipart file uploads or JSON text ingestion.

The legacy synchronous ingestion endpoints have been removed. Keep file and text ingestion behavior through the async RAG document ingestion flow.

## Multimodal Constraints

Current multimodal ingestion extracts image-related knowledge from PDF, DOCX, and Markdown.

Tracked metadata includes:

- image URL
- page number where available
- page coordinate where available
- section title
- image caption and image number
- surrounding text
- structured visual-model JSON

Important limitation:

- The current LangChain4j `MilvusEmbeddingStore` version uses a single vector field and does not expose a custom vector field name. The project currently stores image knowledge as text-searchable structured content. True `image_vector` support requires a visual embedding model and native Milvus multi-vector schema support.

## Chunk Management

Chunk management is split between Redis registry and Milvus vectors.

Current APIs live under:

```text
/api/rag/chunks
```

Updating, disabling, deleting, and rollback create versioned records. Superseded active vectors are removed from Milvus so retrieval does not hit stale content.

When adding new chunk behavior, preserve these fields:

- `chunkId`
- `documentId`
- `contentType`
- `version`
- `status`
- `current`
- `textVectorIds`
- `imageUrl`
- `pageNo`
- `sectionTitle`
- `imageCaption`
- `imageNumber`
- `metadataJson`

## Agent Workflow

Use plan-and-execute for non-trivial work:

1. Read the relevant code and docs first.
2. Create or update a short plan.
3. Implement narrowly.
4. Run the smallest useful verification, then the broader verification if feasible.
5. Summarize changed files, behavior, and any remaining risk.

For SDD work:

- Treat `spec.md` and `docs/spec-implementation-plan.md` as the source of expected behavior.
- Add deterministic unit tests for pure logic such as hashing, idempotency, state transitions, metadata generation, and DTO conversion if needed.
- Avoid integration tests that require live MySQL, Milvus, Redis, RabbitMQ, or model APIs unless the user explicitly asks for them.

## Build And Run Commands

Use JDK 21. This machine has `JAVA_HOME` pointing to JDK 8, which will not work for this project.

PowerShell example:

```powershell
$env:JAVA_HOME='D:\develop\java\jdk21.0.11_10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -q -DskipTests compile
mvn spring-boot:run
```

The project includes `.mvn/maven.config`, which points Maven to the official Maven Central settings file:

```text
.mvn/official-settings.xml
```

Frontend:

```powershell
cd ui
npm install
npm run build
npm run dev
```

Run infrastructure:

```powershell
docker compose -f rag-env/docker-compose.yml up -d
```

`rag-env/docker-compose.yml` includes Redis, RabbitMQ, Milvus, MinIO, etcd, and Attu.

## Configuration

Primary runtime config:

```text
src/main/resources/application-prod.yml
```

Prefer environment variables for secrets and external endpoints. Do not hard-code new API keys, passwords, or tenant-specific values.

Common variables:

- `RAG_LLM_BASE_URL`
- `RAG_LLM_API_KEY`
- `RAG_EMBED_BASE_URL`
- `RAG_EMBED_API_KEY`
- `MILVUS_HOST`
- `REDIS_HOST`
- `RABBITMQ_HOST`
- `RAG_OBJECT_DIR`
- `RAG_MULTIMODAL_INGEST_ENABLED`
- `RAG_VISION_ANALYSIS_ENABLED`

## API Groups

Chat:

- `GET /api/chat`
- `GET /api/chat/detail`
- `GET /api/chat/stream`

RAG query/search:

- `POST /api/rag/query`
- `POST /api/rag/search`

RAG metadata ingestion:

- `POST /api/rag/documents/ingest`
- `POST /api/rag/knowledge-bases/{knowledgeBaseId}/documents/ingest`
- `GET /api/rag/ingestion-tasks/{taskId}`
- `GET /api/rag/documents/{documentId}`

Knowledge bases:

- `GET /api/rag/knowledge-bases`
- `GET /api/rag/knowledge-bases/{id}`
- `POST /api/rag/knowledge-bases`

Chunk management:

- `GET /api/rag/chunks`
- `GET /api/rag/chunks/{chunkId}`
- `GET /api/rag/chunks/{chunkId}/versions`
- `POST /api/rag/chunks`
- `PUT /api/rag/chunks/{chunkId}`
- `POST /api/rag/chunks/{chunkId}/rollback`
- `POST /api/rag/chunks/{chunkId}/disable`
- `DELETE /api/rag/chunks/{chunkId}`

Model configuration:

- `GET /api/admin/model-configs`
- `GET /api/admin/model-configs/{id}`
- `POST /api/admin/model-configs`
- `PUT /api/admin/model-configs/{id}`
- `DELETE /api/admin/model-configs/{id}`
- `GET /api/admin/model-configs/cache-stats`
- `POST /api/admin/model-configs/cache-stats/reset`

Milvus and vector stores:

- `/api/vector-stores/**`
- `/api/milvus/collections/**`

## Coding Guidelines

- Keep changes scoped to the requested behavior.
- Preserve existing public APIs unless the user explicitly approves breaking changes.
- Do not edit generated files under `target/`.
- Use MyBatis-Plus patterns already present in the project.
- Keep deterministic pure logic in small classes under `service/ragops/` when possible.
- Keep external system integration behind services so it can be replaced or mocked later.
- Avoid putting full document text into Milvus metadata payload unless explicitly needed; MySQL should remain the source for full chunk content.
- For document/chunk updates, preserve vector ID mapping and final consistency semantics.
- Do not silently swallow ingestion failures; update task/document error fields.
- Remove null metadata fields before passing metadata to vector-store APIs.
- All file encoding must be UTF-8 (no BOM).
- Use environment variables for configuration values; avoid hard-coded values.

## Security Guidelines

- Use `LogMasker` utility to mask sensitive data (API keys, tokens) in logs.
- Avoid SQL/JSON string concatenation; use parameterized queries and ObjectNode.
- Never log raw API keys or credentials.
- Follow the principle of least privilege for database and external service access.

## Optimization Improvements

The following optimizations have been implemented:

- **Log Sensitive Information Leakage**: `LogMasker` utility masks API keys, tokens, and sensitive data in logs.
- **SQL/JSON Injection Risk**: Replaced string concatenation with ObjectNode for JSON query building.
- **Scheduled Task Thread Pool**: Custom `ThreadPoolTaskScheduler` configured for parallel task execution.
- **Blocking Polling**: Replaced `Thread.sleep` with non-blocking `PollingExecutor` using `ScheduledExecutorService`.
- **Empty Catch Blocks**: Added proper logging to all empty catch blocks.
- **Cache Consistency**: Enhanced `DynamicModelFactory` with cache statistics (hit/miss/creation/expiration counts).
- **Hardcoded Values**: All model configuration values read from database or `RagProperties`.

## Verification Checklist

Before finalizing backend changes:

```powershell
mvn -q -DskipTests compile
```

Before finalizing frontend changes:

```powershell
cd ui
npm run build
```

For ingestion-related changes, also review:

- `src/main/resources/sql/all-in-one.sql`
- `docs/spec-implementation-plan.md`
- `docs/chunk-management.md`
- `KnowledgeIngestionService`
- `RagIngestionExecutor`
- `KnowledgeChunkManagementService`

## Current Project State

- ✅ All files encoded in UTF-8 (no BOM)
- ✅ Test code removed (`src/test/` directory deleted)
- ✅ Hardcoded values eliminated
- ✅ Java version: 21
- ✅ Spring Boot version: 3.2.6
- ✅ MyBatis-Plus version: 3.5.15
- ✅ LangChain4j version: 0.35.0

package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.*;
import cc.ivera.ragdemo.mapper.RagDocumentChunkMapper;
import cc.ivera.ragdemo.mapper.RagDocumentMapper;
import cc.ivera.ragdemo.mapper.RagDocumentVersionMapper;
import cc.ivera.ragdemo.mapper.RagIngestionTaskMapper;
import cc.ivera.ragdemo.model.knowledge.KnowledgeChunkRecord;
import cc.ivera.ragdemo.model.knowledge.KnowledgeIngestionProgressListener;
import cc.ivera.ragdemo.model.knowledge.KnowledgeIngestionResult;
import cc.ivera.ragdemo.model.knowledge.RagIngestionTaskMessage;
import cc.ivera.ragdemo.service.query.KeywordSearchIndex;
import cc.ivera.ragdemo.service.ragops.*;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagIngestionExecutor {

    private static final Logger log = LoggerFactory.getLogger(RagIngestionExecutor.class);

    private final RagIngestionTaskMapper taskMapper;
    private final RagDocumentMapper documentMapper;
    private final RagDocumentVersionMapper versionMapper;
    private final RagDocumentChunkMapper chunkMapper;
    private final RagKnowledgeBaseService knowledgeBaseService;
    private final ObjectStorageService objectStorageService;
    private final KnowledgeIngestionService knowledgeIngestionService;
    private final RagProperties properties;
    private final KnowledgeChunkEntityConverter chunkEntityConverter;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final KeywordSearchIndex keywordSearchIndex;
    private final IngestionTaskProgressService progressService;
    private final IngestionTaskCancellationService cancellationService;
    private final IngestionShardRetryService shardRetryService;
    private final MetricsService metricsService;

    public void execute(RagIngestionTaskMessage message) {
        if (message == null || message.taskId() == null) {
            throw new IllegalArgumentException("Ingestion task message is required");
        }
        execute(message.taskId(), message);
    }

    public void execute(Long taskId) {
        execute(taskId, null);
    }

    private void execute(Long taskId, RagIngestionTaskMessage message) {
        long startedNanos = System.nanoTime();
        boolean success = false;
        try {
            RagIngestionTask task = taskMapper.selectById(taskId);
            if (task == null) {
                log.warn("Ingestion task not found: {}, likely deleted or expired. Discarding message to avoid infinite redelivery.", taskId);
                return;
            }
            validateMessageScope(task, message);
            progressService.initializeStages(task, IngestionStageProgressPolicy.defaultStages());
            RagDocument document = documentMapper.selectById(task.getDocumentId());
            if (document == null) {
                markTaskFailed(task, "DOCUMENT_NOT_FOUND", "Document not found: " + task.getDocumentId());
                return;
            }
            RagKnowledgeBase kb = knowledgeBaseService.getRequired(task.getKnowledgeBaseId());
            validateEntityScope(task, document, kb, message);
            RagDocumentVersion version = task.getDocumentVersionId() == null ? null : versionMapper.selectById(task.getDocumentVersionId());

            try {
                transitionTask(task, IngestionTaskStatus.RUNNING);
                progressService.startTask(taskId);
                markDocumentRunning(document, version);

                cancellationService.throwIfCancellationRequested(taskId);
                progressService.startStage(taskId, IngestionStageProgressPolicy.OBJECT_READ);
                String objectKey = version == null ? document.getObjectKey() : version.getObjectKey();
                String originalFilename = version == null ? document.getOriginalFilename() : version.getOriginalFilename();
                byte[] bytes = objectStorageService.read(objectKey);
                progressService.completeStage(taskId, IngestionStageProgressPolicy.OBJECT_READ, 1, 0, 1);

                cancellationService.throwIfCancellationRequested(taskId);
                KnowledgeIngestionResult result = knowledgeIngestionService.ingestFileBytes(
                        originalFilename,
                        bytes,
                        java.util.Map.of(
                                "source", "file",
                                "fileName", originalFilename,
                                "document_id", document.getDocumentUid(),
                                "document_db_id", String.valueOf(document.getId()),
                                "document_version_id", version == null ? "0" : String.valueOf(version.getId()),
                                "knowledge_base_id", String.valueOf(kb.getId()),
                                "tenant_id", String.valueOf(kb.getTenantId())
                        ),
                        progressListener(taskId)
                );

                cancellationService.throwIfCancellationRequested(taskId);
                PersistChunksResult persistResult = persistChunks(taskId, kb, document, version, result.chunkRecords());
                if (persistResult.failed() > 0 && persistResult.success() == 0) {
                    throw new IllegalStateException("All ingestion shards failed during metadata persistence");
                }
                supersedeOtherVersionChunks(document, version);
                markDocumentSuccess(document, version, result);
                if (persistResult.failed() > 0) {
                    markTaskPartialSuccess(task, persistResult.total(), persistResult.success(), persistResult.failed());
                } else {
                    markTaskSuccess(task, result.chunks());
                }
                success = true;
            } catch (IngestionTaskCancelledException e) {
                markDocumentCancelled(document, version);
                cancellationService.markCancelled(taskId);
            } catch (Exception e) {
                String currentStage = currentStage(taskId);
                if (StringUtils.hasText(currentStage)) {
                    progressService.failStage(taskId, currentStage, "INGESTION_FAILED", e.getMessage());
                }
                markDocumentFailed(document, version, e.getMessage());
                markTaskFailed(task, "INGESTION_FAILED", e.getMessage());
            }
        } finally {
            metricsService.recordIngestion(
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos),
                    success);
        }
    }

    private void validateMessageScope(RagIngestionTask task, RagIngestionTaskMessage message) {
        if (message == null) {
            return;
        }
        if (message.tenantId() != null && !message.tenantId().equals(task.getTenantId())) {
            throw new IllegalStateException("Ingestion message tenant does not match task tenant");
        }
        if (message.documentId() != null && !message.documentId().equals(task.getDocumentId())) {
            throw new IllegalStateException("Ingestion message document does not match task document");
        }
        if (message.knowledgeBaseId() != null && !message.knowledgeBaseId().equals(task.getKnowledgeBaseId())) {
            throw new IllegalStateException("Ingestion message knowledge base does not match task knowledge base");
        }
        if (message.documentVersionId() != null && !message.documentVersionId().equals(task.getDocumentVersionId())) {
            throw new IllegalStateException("Ingestion message document version does not match task version");
        }
    }

    private void validateEntityScope(RagIngestionTask task,
                                     RagDocument document,
                                     RagKnowledgeBase kb,
                                     RagIngestionTaskMessage message) {
        Long tenantId = task.getTenantId();
        if (!tenantId.equals(document.getTenantId()) || !tenantId.equals(kb.getTenantId())) {
            throw new IllegalStateException("Ingestion task, document and knowledge base tenant scope mismatch");
        }
        if (!task.getKnowledgeBaseId().equals(document.getKnowledgeBaseId())) {
            throw new IllegalStateException("Ingestion task and document knowledge base mismatch");
        }
        if (message != null && message.tenantId() != null && !tenantId.equals(message.tenantId())) {
            throw new IllegalStateException("Ingestion message tenant mismatch");
        }
    }

    private PersistChunksResult persistChunks(Long taskId,
                                              RagKnowledgeBase kb,
                                              RagDocument document,
                                              RagDocumentVersion version,
                                              List<KnowledgeChunkRecord> records) {
        progressService.startStage(taskId, IngestionStageProgressPolicy.PERSIST_METADATA);
        int index = 0;
        int success = 0;
        int failed = 0;
        for (KnowledgeChunkRecord record : records) {
            cancellationService.throwIfCancellationRequested(taskId);
            String shardType = "image".equalsIgnoreCase(record.contentType()) ? "IMAGE_ASSET" : "TEXT_CHUNK";
            shardRetryService.markShardRunning(
                    taskId,
                    IngestionStageProgressPolicy.PERSIST_METADATA,
                    document.getId(),
                    version == null ? 0L : version.getId(),
                    record.chunkId(),
                    shardType,
                    index,
                    RagHashing.sha256Hex(record.textContent())
            );
            try {
                RagDocumentChunk chunk = chunkEntityConverter.toEntity(record);
                chunk.setTenantId(kb.getTenantId());
                chunk.setKnowledgeBaseId(kb.getId());
                chunk.setDocumentId(document.getId());
                chunk.setSourceDocumentId(document.getDocumentUid());
                chunk.setDocumentVersionId(version == null ? 0L : version.getId());
                chunk.setChunkIndex(index);
                chunk.setVectorStoreType("milvus");
                chunk.setEmbeddingModel(properties.getEmbedding().getModel());
                chunk.setEmbeddingDimension(properties.getEmbedding().getDimension());
                chunk.setEmbeddingStatus(2);
                chunk.setIsDeleted(0);
                chunk.setCreatedAt(LocalDateTime.now());
                chunk.setUpdatedAt(LocalDateTime.now());
                upsertChunk(chunk);
                shardRetryService.markShardSuccess(taskId, record.chunkId(), shardType, chunk.getVectorId());
                success++;
            } catch (Exception e) {
                shardRetryService.markShardFailed(taskId, record.chunkId(), shardType, e);
                failed++;
                log.warn("Failed to persist ingestion shard {} for task {}: {}", record.chunkId(), taskId, e.getMessage());
            } finally {
                index++;
                progressService.updateStageProgress(taskId, IngestionStageProgressPolicy.PERSIST_METADATA, success, failed, records.size());
            }
        }
        progressService.completeStage(taskId, IngestionStageProgressPolicy.PERSIST_METADATA, success, failed, records.size());
        if (keywordSearchIndex == null || !keywordSearchIndex.enabled()) {
            progressService.skipStage(taskId, IngestionStageProgressPolicy.SYNC_KEYWORD_INDEX, "Keyword index is disabled");
        } else {
            progressService.completeStage(taskId, IngestionStageProgressPolicy.SYNC_KEYWORD_INDEX, success, failed, records.size());
        }
        return new PersistChunksResult(records.size(), success, failed);
    }

    private void supersedeOtherVersionChunks(RagDocument document, RagDocumentVersion version) {
        if (version == null) {
            return;
        }
        List<RagDocumentChunk> oldChunks = chunkMapper.selectList(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getDocumentId, document.getId())
                .ne(RagDocumentChunk::getDocumentVersionId, version.getId())
                .eq(RagDocumentChunk::getCurrentFlag, true));
        List<String> vectorIds = oldChunks.stream()
                .map(RagDocumentChunk::getVectorId)
                .filter(StringUtils::hasText)
                .toList();
        if (!vectorIds.isEmpty()) {
            dynamicMilvusStoreManager.current().store().removeAll(vectorIds);
        }
        if (!oldChunks.isEmpty()) {
            oldChunks.forEach(this::deleteKeywordIndex);
            chunkMapper.update(null, new LambdaUpdateWrapper<RagDocumentChunk>()
                    .eq(RagDocumentChunk::getDocumentId, document.getId())
                    .ne(RagDocumentChunk::getDocumentVersionId, version.getId())
                    .eq(RagDocumentChunk::getCurrentFlag, true)
                    .set(RagDocumentChunk::getCurrentFlag, false)
                    .set(RagDocumentChunk::getChunkStatus, "SUPERSEDED")
                    .set(RagDocumentChunk::getUpdatedAt, LocalDateTime.now()));
        }
    }

    private void upsertChunk(RagDocumentChunk chunk) {
        RagDocumentChunk existing = chunkMapper.selectOne(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getChunkUid, chunk.getChunkUid())
                .eq(RagDocumentChunk::getChunkVersion, chunk.getChunkVersion())
                .last("LIMIT 1"));
        if (existing == null) {
            chunkMapper.insert(chunk);
            upsertKeywordIndex(chunk);
            return;
        }
        chunk.setId(existing.getId());
        chunkMapper.updateById(chunk);
        upsertKeywordIndex(chunk);
    }

    private void upsertKeywordIndex(RagDocumentChunk chunk) {
        if (keywordSearchIndex == null || !keywordSearchIndex.enabled()) {
            return;
        }
        try {
            keywordSearchIndex.upsert(chunk);
        } catch (Exception e) {
            log.warn("Failed to upsert keyword index for chunk {}: {}", chunk.getChunkUid(), e.getMessage());
        }
    }

    private void deleteKeywordIndex(RagDocumentChunk chunk) {
        if (keywordSearchIndex == null || !keywordSearchIndex.enabled()) {
            return;
        }
        try {
            keywordSearchIndex.delete(chunk.getChunkUid());
        } catch (Exception e) {
            log.warn("Failed to delete keyword index for chunk {}: {}", chunk.getChunkUid(), e.getMessage());
        }
    }

    private void transitionTask(RagIngestionTask task, IngestionTaskStatus to) {
        IngestionTaskStatus from = IngestionTaskStatus.fromCode(task.getTaskStatus());
        IngestionTaskStateMachine.assertTransit(from, to);
        task.setTaskStatus(to.code());
        task.setProgress(to == IngestionTaskStatus.RUNNING ? 10 : task.getProgress());
        task.setCancelRequested(false);
        task.setCancelRequestedAt(null);
        task.setCancelRequestedBy(null);
        task.setPartialSuccess(false);
        task.setStartedAt(task.getStartedAt() == null ? LocalDateTime.now() : task.getStartedAt());
        task.setHeartbeatAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private void markDocumentRunning(RagDocument document, RagDocumentVersion version) {
        document.setParseStatus(1);
        document.setChunkStatus(1);
        document.setEmbeddingStatus(1);
        document.setDocumentStatus(0);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        if (version != null) {
            version.setParseStatus(1);
            version.setChunkStatus(1);
            version.setEmbeddingStatus(1);
            version.setVersionStatus(DocumentVersionStatus.PROCESSING.code());
            version.setUpdatedAt(LocalDateTime.now());
            versionMapper.updateById(version);
        }
    }

    private void markDocumentSuccess(RagDocument document, RagDocumentVersion version, KnowledgeIngestionResult result) {
        document.setParseStatus(2);
        document.setChunkStatus(2);
        document.setEmbeddingStatus(2);
        document.setDocumentStatus(1);
        document.setChunkCount(result.chunks());
        document.setCharacterCount((long) result.chunkRecords().stream()
                .map(KnowledgeChunkRecord::textContent)
                .filter(text -> text != null)
                .mapToInt(String::length)
                .sum());
        document.setTokenCount(result.chunkRecords().stream()
                .map(KnowledgeChunkRecord::textContent)
                .mapToLong(this::estimateTokenCount)
                .sum());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        if (version != null) {
            version.setParseStatus(2);
            version.setChunkStatus(2);
            version.setEmbeddingStatus(2);
            version.setVersionStatus(DocumentVersionStatus.AVAILABLE.code());
            version.setChunkCount(result.chunks());
            version.setCharacterCount(document.getCharacterCount());
            version.setTokenCount(document.getTokenCount());
            version.setErrorCode(null);
            version.setErrorMessage(null);
            version.setUpdatedAt(LocalDateTime.now());
            versionMapper.updateById(version);
        }
    }

    private void markDocumentFailed(RagDocument document, RagDocumentVersion version, String message) {
        document.setParseStatus(3);
        document.setChunkStatus(3);
        document.setEmbeddingStatus(3);
        document.setDocumentStatus(2);
        document.setErrorCode("INGESTION_FAILED");
        document.setErrorMessage(truncate(message, 1900));
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        if (version != null) {
            version.setParseStatus(3);
            version.setChunkStatus(3);
            version.setEmbeddingStatus(3);
            version.setVersionStatus(DocumentVersionStatus.FAILED.code());
            version.setErrorCode("INGESTION_FAILED");
            version.setErrorMessage(truncate(message, 1900));
            version.setUpdatedAt(LocalDateTime.now());
            versionMapper.updateById(version);
        }
    }

    private void markDocumentCancelled(RagDocument document, RagDocumentVersion version) {
        document.setParseStatus(3);
        document.setChunkStatus(3);
        document.setEmbeddingStatus(3);
        document.setDocumentStatus(2);
        document.setErrorCode("TASK_CANCELLED");
        document.setErrorMessage("Ingestion task was cancelled");
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        if (version != null) {
            version.setParseStatus(3);
            version.setChunkStatus(3);
            version.setEmbeddingStatus(3);
            version.setVersionStatus(DocumentVersionStatus.FAILED.code());
            version.setErrorCode("TASK_CANCELLED");
            version.setErrorMessage("Ingestion task was cancelled");
            version.setUpdatedAt(LocalDateTime.now());
            versionMapper.updateById(version);
        }
    }

    private void markTaskSuccess(RagIngestionTask task, int total) {
        task.setTaskStatus(IngestionTaskStatus.SUCCESS.code());
        task.setProgress(100);
        task.setStageProgress(100);
        task.setCurrentStage(null);
        task.setTotalCount(total);
        task.setSuccessCount(total);
        task.setFailedCount(0);
        task.setPartialSuccess(false);
        task.setFinishedAt(LocalDateTime.now());
        task.setHeartbeatAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        progressService.emitTaskTerminalEvent(task.getId(), "TASK_SUCCEEDED", "Ingestion task succeeded");
    }

    private void markTaskPartialSuccess(RagIngestionTask task, int total, int success, int failed) {
        task.setTaskStatus(IngestionTaskStatus.PARTIAL_SUCCESS.code());
        task.setProgress(100);
        task.setStageProgress(100);
        task.setCurrentStage(null);
        task.setTotalCount(total);
        task.setSuccessCount(success);
        task.setFailedCount(failed);
        task.setPartialSuccess(true);
        task.setErrorCode("PARTIAL_SUCCESS");
        task.setErrorMessage("Some ingestion shards failed and can be retried");
        task.setFinishedAt(LocalDateTime.now());
        task.setHeartbeatAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        progressService.emitTaskTerminalEvent(task.getId(), "TASK_PARTIAL_SUCCESS", task.getErrorMessage());
    }

    private void markTaskFailed(RagIngestionTask task, String code, String message) {
        task.setTaskStatus(IngestionTaskStatus.FAILED.code());
        task.setErrorCode(code);
        task.setErrorMessage(truncate(message, 1900));
        task.setFailedCount(task.getFailedCount() == null ? 1 : task.getFailedCount() + 1);
        task.setFinishedAt(LocalDateTime.now());
        task.setHeartbeatAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        progressService.emitTaskTerminalEvent(task.getId(), "TASK_FAILED", truncate(message, 900));
    }

    private KnowledgeIngestionProgressListener progressListener(Long taskId) {
        return new KnowledgeIngestionProgressListener() {
            @Override
            public void checkCancelled() {
                cancellationService.throwIfCancellationRequested(taskId);
            }

            @Override
            public void stageStarted(String stageCode) {
                progressService.startStage(taskId, stageCode);
            }

            @Override
            public void stageProgress(String stageCode, int successCount, int failedCount, int totalCount) {
                progressService.updateStageProgress(taskId, stageCode, successCount, failedCount, totalCount);
            }

            @Override
            public void stageCompleted(String stageCode, int successCount, int failedCount, int totalCount) {
                progressService.completeStage(taskId, stageCode, successCount, failedCount, totalCount);
            }

            @Override
            public void stageSkipped(String stageCode, String message) {
                progressService.skipStage(taskId, stageCode, message);
            }
        };
    }

    private String currentStage(Long taskId) {
        RagIngestionTask current = taskMapper.selectById(taskId);
        return current == null ? null : current.getCurrentStage();
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value) || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }

    private record PersistChunksResult(int total, int success, int failed) {
    }
}

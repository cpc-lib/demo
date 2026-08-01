package cc.ivera.ragdemo.model.dto;

import cc.ivera.ragdemo.domain.rag.*;
import cc.ivera.ragdemo.domain.tenant.RagTenantModelConfig;
import cc.ivera.ragdemo.domain.tenant.RagTenantQuota;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Entity 到 DTO 的转换器
 * 在 Controller 层使用，避免直接暴露内部实体结构
 */
@Component
public class EntityDtoConverter {

    public RagDocumentDto toDto(RagDocument entity) {
        if (entity == null) {
            return null;
        }
        return new RagDocumentDto(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getDocumentUid(),
                entity.getDocumentName(),
                entity.getSourceType(),
                entity.getSourceUri(),
                entity.getObjectKey(),
                entity.getOriginalFilename(),
                entity.getFileExtension(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getFileHash(),
                entity.getCurrentVersionId(),
                entity.getCurrentVersionNo(),
                entity.getPageCount(),
                entity.getChunkCount(),
                entity.getCharacterCount(),
                entity.getTokenCount(),
                entity.getParseStatus(),
                entity.getChunkStatus(),
                entity.getEmbeddingStatus(),
                entity.getDocumentStatus(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getMetadataJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RagDocumentDto> toDocumentDtoList(List<RagDocument> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }

    public RagDocumentVersionDto toDto(RagDocumentVersion entity) {
        if (entity == null) {
            return null;
        }
        return new RagDocumentVersionDto(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getDocumentId(),
                entity.getVersionNo(),
                entity.getVersionUid(),
                entity.getDocumentName(),
                entity.getSourceType(),
                entity.getSourceUri(),
                entity.getObjectKey(),
                entity.getOriginalFilename(),
                entity.getFileExtension(),
                entity.getMimeType(),
                entity.getFileSize(),
                entity.getFileHash(),
                entity.getPageCount(),
                entity.getChunkCount(),
                entity.getCharacterCount(),
                entity.getTokenCount(),
                entity.getParseStatus(),
                entity.getChunkStatus(),
                entity.getEmbeddingStatus(),
                entity.getVersionStatus(),
                entity.getCurrentFlag(),
                entity.getVersionNote(),
                entity.getApprovalStatus(),
                entity.getApprovalComment(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getPublishedAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getMetadataJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RagDocumentVersionDto> toDocumentVersionDtoList(List<RagDocumentVersion> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }

    public RagIngestionTaskDto toDto(RagIngestionTask entity) {
        if (entity == null) {
            return null;
        }
        return new RagIngestionTaskDto(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getDocumentId(),
                entity.getDocumentVersionId(),
                entity.getTaskNo(),
                entity.getTaskType(),
                entity.getTaskStatus(),
                entity.getProgress(),
                entity.getCurrentStage(),
                entity.getStageProgress(),
                entity.getTotalCount(),
                entity.getSuccessCount(),
                entity.getFailedCount(),
                entity.getRetryCount(),
                entity.getMaxRetryCount(),
                entity.getNextRetryAt(),
                entity.getCancelRequested(),
                entity.getCancelRequestedAt(),
                entity.getCancelRequestedBy(),
                entity.getPartialSuccess(),
                entity.getLastEventId(),
                entity.getHeartbeatAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getTraceId(),
                entity.getIdempotencyKey(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RagIngestionTaskDto> toIngestionTaskDtoList(List<RagIngestionTask> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }

    public RagIngestionTaskShardDto toDto(RagIngestionTaskShard entity) {
        if (entity == null) {
            return null;
        }
        return new RagIngestionTaskShardDto(
                entity.getId(),
                entity.getTaskId(),
                entity.getStageCode(),
                entity.getDocumentId(),
                entity.getDocumentVersionId(),
                entity.getShardKey(),
                entity.getShardType(),
                entity.getShardIndex(),
                entity.getShardStatus(),
                entity.getRetryCount(),
                entity.getMaxRetryCount(),
                entity.getNextRetryAt(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getInputHash(),
                entity.getOutputRef(),
                entity.getMetadataJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RagIngestionTaskShardDto> toIngestionTaskShardDtoList(List<RagIngestionTaskShard> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }

    public RagKnowledgeBaseDto toDto(RagKnowledgeBase entity) {
        if (entity == null) {
            return null;
        }
        return new RagKnowledgeBaseDto(
                entity.getId(),
                entity.getKbCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getVectorStoreType(),
                entity.getVectorCollection(),
                entity.getEmbeddingModel(),
                entity.getEmbeddingDimension(),
                entity.getChunkStrategy(),
                entity.getChunkSize(),
                entity.getChunkOverlap(),
                entity.getRetrievalTopK(),
                entity.getMinScore(),
                entity.getConfigJson(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RagKnowledgeBaseDto> toKnowledgeBaseDtoList(List<RagKnowledgeBase> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }

    public RagImageAssetDto toDto(RagImageAsset entity) {
        if (entity == null) {
            return null;
        }
        return new RagImageAssetDto(
                entity.getId(),
                entity.getKnowledgeBaseId(),
                entity.getDocumentId(),
                entity.getDocumentVersionId(),
                entity.getSourceDocumentId(),
                entity.getImageId(),
                entity.getChunkUid(),
                entity.getContentType(),
                entity.getAssetPath(),
                entity.getImageUrl(),
                entity.getPageNo(),
                entity.getCoordinateJson(),
                entity.getSectionTitle(),
                entity.getImageCaption(),
                entity.getImageNumber(),
                entity.getOcrText(),
                entity.getOcrStatus(),
                entity.getOcrConfidence(),
                entity.getOcrProvider(),
                entity.getOcrModel(),
                entity.getOcrErrorMessage(),
                entity.getVisualStatus(),
                entity.getVisualSchemaValid(),
                entity.getVisualConfidence(),
                entity.getVisualJson(),
                entity.getVisualSchemaErrors(),
                entity.getTextVectorIds(),
                entity.getImageVectorIds(),
                entity.getImageEmbeddingStatus(),
                entity.getImageEmbeddingModel(),
                entity.getImageEmbeddingDimension(),
                entity.getImageEmbeddingErrorMessage(),
                entity.getImageEmbeddingUpdatedAt(),
                entity.getReviewStatus(),
                entity.getReviewComment(),
                entity.getReviewedBy(),
                entity.getReviewedAt(),
                entity.getReviewUpdatedVisualJson(),
                entity.getReviewUpdatedOcrText(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RagImageAssetDto> toImageAssetDtoList(List<RagImageAsset> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }

    public RagAgentPromptDto toDto(RagAgentPrompt entity) {
        if (entity == null) {
            return null;
        }
        return new RagAgentPromptDto(
                entity.getId(),
                entity.getTenantId(),
                entity.getPromptName(),
                entity.getPromptContent(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RagAgentPromptDto> toAgentPromptDtoList(List<RagAgentPrompt> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }

    public RagTenantQuotaDto toDto(RagTenantQuota entity) {
        if (entity == null) {
            return null;
        }
        return new RagTenantQuotaDto(
                entity.getId(),
                entity.getMaxDocuments(),
                entity.getMaxStorageBytes(),
                entity.getMaxFileBytes(),
                entity.getDailyOcrLimit(),
                entity.getDailyEmbeddingTokens(),
                entity.getMaxConcurrentIngestionTasks(),
                entity.getDailyQueryLimit(),
                entity.getMonthlyBudgetCents(),
                entity.getEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public RagTenantModelConfigDto toDto(RagTenantModelConfig entity) {
        if (entity == null) {
            return null;
        }
        return new RagTenantModelConfigDto(
                entity.getId(),
                entity.getProvider(),
                entity.getModelType(),
                entity.getModelName(),
                entity.getBaseUrl(),
                entity.getTemperature(),
                entity.getDimension(),
                entity.getImageSize(),
                entity.getImageQuality(),
                entity.getPollIntervalMillis(),
                entity.getRateLimitQps(),
                entity.getMonthlyBudgetCents(),
                entity.getApiKeySecretRef() != null && !entity.getApiKeySecretRef().isBlank(),
                entity.getTimeoutSeconds(),
                entity.getMaxRetries(),
                entity.getMaxTokens(),
                entity.getFrequencyPenalty(),
                entity.getPresencePenalty(),
                entity.getTopP(),
                entity.getEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public List<RagTenantModelConfigDto> toTenantModelConfigDtoList(List<RagTenantModelConfig> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toDto).toList();
    }
}

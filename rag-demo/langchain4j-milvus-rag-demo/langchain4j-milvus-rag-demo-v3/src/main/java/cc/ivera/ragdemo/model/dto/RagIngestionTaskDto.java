package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagIngestionTaskDto(
        Long id,
        Long knowledgeBaseId,
        Long documentId,
        Long documentVersionId,
        String taskNo,
        String taskType,
        Integer taskStatus,
        Integer progress,
        String currentStage,
        Integer stageProgress,
        Integer totalCount,
        Integer successCount,
        Integer failedCount,
        Integer retryCount,
        Integer maxRetryCount,
        LocalDateTime nextRetryAt,
        Boolean cancelRequested,
        LocalDateTime cancelRequestedAt,
        String cancelRequestedBy,
        Boolean partialSuccess,
        Long lastEventId,
        LocalDateTime heartbeatAt,
        String errorCode,
        String errorMessage,
        String traceId,
        String idempotencyKey,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

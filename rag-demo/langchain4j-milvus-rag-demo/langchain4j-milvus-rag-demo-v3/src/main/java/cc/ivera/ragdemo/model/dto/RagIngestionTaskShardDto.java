package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagIngestionTaskShardDto(
        Long id,
        Long taskId,
        String stageCode,
        Long documentId,
        Long documentVersionId,
        String shardKey,
        String shardType,
        Integer shardIndex,
        String shardStatus,
        Integer retryCount,
        Integer maxRetryCount,
        LocalDateTime nextRetryAt,
        String errorCode,
        String errorMessage,
        String inputHash,
        String outputRef,
        String metadataJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

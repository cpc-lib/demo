package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagDocumentDto(
        Long id,
        Long knowledgeBaseId,
        String documentUid,
        String documentName,
        Integer sourceType,
        String sourceUri,
        String objectKey,
        String originalFilename,
        String fileExtension,
        String mimeType,
        Long fileSize,
        String fileHash,
        Long currentVersionId,
        Integer currentVersionNo,
        Integer pageCount,
        Integer chunkCount,
        Long characterCount,
        Long tokenCount,
        Integer parseStatus,
        Integer chunkStatus,
        Integer embeddingStatus,
        Integer documentStatus,
        String errorCode,
        String errorMessage,
        String metadataJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

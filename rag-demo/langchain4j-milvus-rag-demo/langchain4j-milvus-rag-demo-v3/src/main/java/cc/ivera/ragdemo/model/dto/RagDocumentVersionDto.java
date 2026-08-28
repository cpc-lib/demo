package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagDocumentVersionDto(
        Long id,
        Long knowledgeBaseId,
        Long documentId,
        Integer versionNo,
        String versionUid,
        String documentName,
        Integer sourceType,
        String sourceUri,
        String objectKey,
        String originalFilename,
        String fileExtension,
        String mimeType,
        Long fileSize,
        String fileHash,
        Integer pageCount,
        Integer chunkCount,
        Long characterCount,
        Long tokenCount,
        Integer parseStatus,
        Integer chunkStatus,
        Integer embeddingStatus,
        Integer versionStatus,
        Boolean currentFlag,
        String versionNote,
        String approvalStatus,
        String approvalComment,
        String approvedBy,
        LocalDateTime approvedAt,
        LocalDateTime publishedAt,
        String errorCode,
        String errorMessage,
        String metadataJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

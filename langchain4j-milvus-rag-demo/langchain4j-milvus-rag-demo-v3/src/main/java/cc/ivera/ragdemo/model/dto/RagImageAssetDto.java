package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagImageAssetDto(
        Long id,
        Long knowledgeBaseId,
        Long documentId,
        Long documentVersionId,
        String sourceDocumentId,
        String imageId,
        String chunkUid,
        String contentType,
        String assetPath,
        String imageUrl,
        Integer pageNo,
        String coordinateJson,
        String sectionTitle,
        String imageCaption,
        String imageNumber,
        String ocrText,
        String ocrStatus,
        Double ocrConfidence,
        String ocrProvider,
        String ocrModel,
        String ocrErrorMessage,
        String visualStatus,
        Boolean visualSchemaValid,
        Double visualConfidence,
        String visualJson,
        String visualSchemaErrors,
        String textVectorIds,
        String imageVectorIds,
        String imageEmbeddingStatus,
        String imageEmbeddingModel,
        Integer imageEmbeddingDimension,
        String imageEmbeddingErrorMessage,
        LocalDateTime imageEmbeddingUpdatedAt,
        String reviewStatus,
        String reviewComment,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewUpdatedVisualJson,
        String reviewUpdatedOcrText,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

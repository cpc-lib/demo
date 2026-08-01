package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagKnowledgeBaseDto(
        Long id,
        String kbCode,
        String name,
        String description,
        String vectorStoreType,
        String vectorCollection,
        String embeddingModel,
        Integer embeddingDimension,
        String chunkStrategy,
        Integer chunkSize,
        Integer chunkOverlap,
        Integer retrievalTopK,
        BigDecimal minScore,
        String configJson,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

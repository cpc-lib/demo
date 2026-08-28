package cc.ivera.ragdemo.controller;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RagImageSearchRequest(
        @NotEmpty(message = "knowledgeBaseIds must not be empty")
        List<Long> knowledgeBaseIds,
        String question,
        String imageUrl,
        Long imageAssetId,
        String imageBase64,
        String retrievalMode,
        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 50, message = "topK must be at most 50")
        Integer topK,
        @DecimalMin(value = "0.0", message = "minScore must be >= 0.0")
        @DecimalMax(value = "1.0", message = "minScore must be <= 1.0")
        Double minScore,
        Boolean includeReviewPending,
        List<String> contentTypes,
        List<String> permissionTags
) {
}

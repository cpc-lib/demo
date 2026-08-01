package cc.ivera.ragdemo.model.query;

import jakarta.validation.constraints.*;

import java.util.List;

public record RagSearchRequest(
        @Deprecated
        Long tenantId,
        @NotEmpty(message = "knowledgeBaseIds must not be empty")
        List<Long> knowledgeBaseIds,
        String query,
        String imageUrl,
        Long imageAssetId,
        String imageBase64,
        List<String> modalities,
        String retrievalMode,
        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 50, message = "topK must be at most 50")
        Integer topK,
        @DecimalMin(value = "0.0", message = "minScore must be >= 0.0")
        @DecimalMax(value = "1.0", message = "minScore must be <= 1.0")
        Double minScore,
        @DecimalMin(value = "0.0", message = "textVectorWeight must be >= 0.0")
        @DecimalMax(value = "1.0", message = "textVectorWeight must be <= 1.0")
        Double textVectorWeight,
        @DecimalMin(value = "0.0", message = "imageVectorWeight must be >= 0.0")
        @DecimalMax(value = "1.0", message = "imageVectorWeight must be <= 1.0")
        Double imageVectorWeight,
        @DecimalMin(value = "0.0", message = "keywordWeight must be >= 0.0")
        @DecimalMax(value = "1.0", message = "keywordWeight must be <= 1.0")
        Double keywordWeight,
        Boolean includeReviewPending,
        List<String> contentTypes,
        List<String> permissionTags
) {

    public RagSearchRequest(Long tenantId,
                            List<Long> knowledgeBaseIds,
                            String query,
                            String retrievalMode,
                            Integer topK,
                            Double minScore,
                            List<String> contentTypes,
                            List<String> permissionTags) {
        this(tenantId, knowledgeBaseIds, query, null, null, null, null, retrievalMode, topK, minScore,
                null, null, null, null, contentTypes, permissionTags);
    }

    @AssertTrue(message = "query or image input must be provided")
    public boolean isQueryOrImageProvided() {
        return hasText(query) || hasText(imageUrl) || hasText(imageBase64) || imageAssetId != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

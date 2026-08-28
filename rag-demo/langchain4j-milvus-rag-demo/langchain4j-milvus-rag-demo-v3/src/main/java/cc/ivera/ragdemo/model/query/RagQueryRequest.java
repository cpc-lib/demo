package cc.ivera.ragdemo.model.query;

import jakarta.validation.constraints.*;

import java.util.List;

public record RagQueryRequest(
        @Deprecated
        Long tenantId,
        @NotEmpty(message = "knowledgeBaseIds must not be empty")
        List<Long> knowledgeBaseIds,
        String question,
        String imageUrl,
        Long imageAssetId,
        String imageBase64,
        List<String> modalities,
        String conversationId,
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
        List<String> permissionTags,
        Boolean enableRewrite,
        Boolean enableRerank,
        Boolean includeSources
) {

    public RagQueryRequest(Long tenantId,
                           List<Long> knowledgeBaseIds,
                           String question,
                           String conversationId,
                           String retrievalMode,
                           Integer topK,
                           Double minScore,
                           List<String> contentTypes,
                           List<String> permissionTags,
                           Boolean enableRewrite,
                           Boolean enableRerank,
                           Boolean includeSources) {
        this(tenantId, knowledgeBaseIds, question, null, null, null, null, conversationId, retrievalMode, topK,
                minScore, null, null, null, null, contentTypes, permissionTags, enableRewrite, enableRerank,
                includeSources);
    }

    @AssertTrue(message = "question or image input must be provided")
    public boolean isQuestionOrImageProvided() {
        return hasText(question) || hasText(imageUrl) || hasText(imageBase64) || imageAssetId != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

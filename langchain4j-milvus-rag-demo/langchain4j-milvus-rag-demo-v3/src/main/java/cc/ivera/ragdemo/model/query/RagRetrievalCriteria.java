package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagRetrievalCriteria(
        String query,
        String imageUrl,
        Long imageAssetId,
        String imageBase64,
        List<String> modalities,
        Long tenantId,
        List<Long> knowledgeBaseIds,
        String retrievalMode,
        Integer topK,
        Double minScore,
        Double textVectorWeight,
        Double imageVectorWeight,
        Double keywordWeight,
        Boolean includeReviewPending,
        List<String> contentTypes,
        List<String> permissionTags
) {

    public RagRetrievalCriteria(String query,
                                Long tenantId,
                                List<Long> knowledgeBaseIds,
                                String retrievalMode,
                                Integer topK,
                                Double minScore,
                                List<String> contentTypes,
                                List<String> permissionTags) {
        this(query, null, null, null, null, tenantId, knowledgeBaseIds, retrievalMode, topK, minScore,
                null, null, null, null, contentTypes, permissionTags);
    }

    public static RagRetrievalCriteria from(RagQueryRequest request) {
        return new RagRetrievalCriteria(
                request.question(),
                request.imageUrl(),
                request.imageAssetId(),
                request.imageBase64(),
                request.modalities(),
                request.tenantId(),
                request.knowledgeBaseIds(),
                request.retrievalMode(),
                request.topK(),
                request.minScore(),
                request.textVectorWeight(),
                request.imageVectorWeight(),
                request.keywordWeight(),
                request.includeReviewPending(),
                request.contentTypes(),
                request.permissionTags()
        );
    }

    public static RagRetrievalCriteria from(RagSearchRequest request) {
        return new RagRetrievalCriteria(
                request.query(),
                request.imageUrl(),
                request.imageAssetId(),
                request.imageBase64(),
                request.modalities(),
                request.tenantId(),
                request.knowledgeBaseIds(),
                request.retrievalMode(),
                request.topK(),
                request.minScore(),
                request.textVectorWeight(),
                request.imageVectorWeight(),
                request.keywordWeight(),
                request.includeReviewPending(),
                request.contentTypes(),
                request.permissionTags()
        );
    }
}

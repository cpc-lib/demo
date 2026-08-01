package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record MultimodalVectorSearchRequest(
        List<Float> vector,
        String modality,
        Long tenantId,
        List<Long> knowledgeBaseIds,
        List<String> contentTypes,
        List<String> permissionTags,
        boolean includeReviewPending,
        int topK,
        double minScore
) {
}

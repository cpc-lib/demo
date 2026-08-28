package cc.ivera.ragdemo.model.knowledge;

import java.util.Map;

public record MultimodalVectorSearchHit(
        String id,
        String chunkUid,
        String imageId,
        Long documentId,
        Long documentVersionId,
        Long knowledgeBaseId,
        Long tenantId,
        String contentType,
        String modality,
        Double score,
        Integer pageNo,
        String sectionTitle,
        Map<String, Object> metadata
) {
}

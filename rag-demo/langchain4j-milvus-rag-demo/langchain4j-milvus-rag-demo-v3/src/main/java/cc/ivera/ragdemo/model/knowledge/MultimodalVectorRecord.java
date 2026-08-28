package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record MultimodalVectorRecord(
        String id,
        String chunkUid,
        String imageId,
        Long documentId,
        Long documentVersionId,
        Long knowledgeBaseId,
        Long tenantId,
        String contentType,
        String modality,
        List<Float> textVector,
        List<Float> imageVector,
        String embeddingModel,
        Integer embeddingDimension,
        Integer pageNo,
        String sectionTitle,
        String permissionTags,
        String reviewStatus,
        boolean current,
        Long createdAt
) {
}

package cc.ivera.ragdemo.controller;

import java.util.List;

public record KnowledgeChunkUpdateRequest(
        String textContent,
        String contentType,
        String imageUrl,
        Integer pageNo,
        String sectionTitle,
        String imageCaption,
        String imageNumber,
        String parentChunkId,
        List<String> permissionTags,
        String tenantId,
        String metadataJson
) {
}

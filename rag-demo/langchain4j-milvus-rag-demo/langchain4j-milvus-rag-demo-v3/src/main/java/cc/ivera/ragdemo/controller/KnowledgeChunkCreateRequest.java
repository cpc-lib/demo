package cc.ivera.ragdemo.controller;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record KnowledgeChunkCreateRequest(
        String documentId,
        String contentType,
        @NotBlank String textContent,
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

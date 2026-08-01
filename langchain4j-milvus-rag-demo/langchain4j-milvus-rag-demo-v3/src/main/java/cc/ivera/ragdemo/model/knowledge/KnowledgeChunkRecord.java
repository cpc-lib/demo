package cc.ivera.ragdemo.model.knowledge;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record KnowledgeChunkRecord(
        String chunkId,
        String documentId,
        String source,
        String fileName,
        String contentType,
        String textContent,
        List<String> textVectorIds,
        List<String> imageVectorIds,
        String imageUrl,
        Integer pageNo,
        String sectionTitle,
        String imageCaption,
        String imageNumber,
        String parentChunkId,
        List<String> permissionTags,
        String tenantId,
        int version,
        ChunkStatus status,
        boolean current,
        String milvusAlias,
        String milvusCollection,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}

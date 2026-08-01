package cc.ivera.ragdemo.model;

import lombok.Builder;

@Builder
public record SourceItem(
        SourceType type,
        String title,
        String url,
        String content,
        String fileName,
        String chunkId,
        Integer version,
        String chunkStatus,
        String contentType,
        String imageUrl,
        Integer pageNo,
        String sectionTitle,
        String imageCaption,
        String imageNumber,
        Double score
) {
}

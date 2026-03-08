package cc.ivera.ragdemo.model;

import lombok.Builder;

@Builder
public record SourceItem(
        SourceType type,
        String title,
        String url,
        String content,
        String fileName,
        Integer chunkId,
        Double score
) {
}

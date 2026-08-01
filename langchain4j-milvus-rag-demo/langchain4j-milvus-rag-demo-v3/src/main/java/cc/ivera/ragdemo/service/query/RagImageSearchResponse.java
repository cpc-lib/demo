package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.RagSearchItem;

import java.util.List;

public record RagImageSearchResponse(
        Long queryLogId,
        List<RagSearchItem> similarImages,
        List<RagSearchItem> relatedKnowledge,
        List<RagSearchItem> items
) {
}

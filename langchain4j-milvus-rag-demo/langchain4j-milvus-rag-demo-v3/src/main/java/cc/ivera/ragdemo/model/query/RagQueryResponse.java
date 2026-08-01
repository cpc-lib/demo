package cc.ivera.ragdemo.model.query;

import java.util.List;

public record RagQueryResponse(
        Long queryLogId,
        String conversationId,
        String answer,
        boolean knowledgeHit,
        List<RagSearchItem> sources,
        RagUsage usage
) {
}

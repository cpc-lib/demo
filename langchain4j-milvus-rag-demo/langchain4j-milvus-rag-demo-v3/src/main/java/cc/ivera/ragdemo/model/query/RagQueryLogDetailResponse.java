package cc.ivera.ragdemo.model.query;

import cc.ivera.ragdemo.domain.rag.RagQueryFeedback;
import cc.ivera.ragdemo.domain.rag.RagQueryHit;
import cc.ivera.ragdemo.domain.rag.RagQueryLog;

import java.util.List;

public record RagQueryLogDetailResponse(
        RagQueryLog log,
        List<RagQueryHit> hits,
        List<RagQueryFeedback> feedbacks
) {
}

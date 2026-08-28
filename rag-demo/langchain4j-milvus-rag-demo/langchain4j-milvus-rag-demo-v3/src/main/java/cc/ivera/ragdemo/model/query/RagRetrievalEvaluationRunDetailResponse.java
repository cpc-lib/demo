package cc.ivera.ragdemo.model.query;

import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalCaseResult;
import cc.ivera.ragdemo.domain.rag.RagRetrievalEvalRun;

import java.util.List;

public record RagRetrievalEvaluationRunDetailResponse(
        RagRetrievalEvalRun run,
        List<RagRetrievalEvalCaseResult> results
) {
}

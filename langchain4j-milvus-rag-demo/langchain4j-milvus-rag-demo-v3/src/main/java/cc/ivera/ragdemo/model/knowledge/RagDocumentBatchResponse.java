package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record RagDocumentBatchResponse(
        String operation,
        int requested,
        int succeeded,
        int failed,
        List<RagDocumentBatchItemResult> results
) {
}

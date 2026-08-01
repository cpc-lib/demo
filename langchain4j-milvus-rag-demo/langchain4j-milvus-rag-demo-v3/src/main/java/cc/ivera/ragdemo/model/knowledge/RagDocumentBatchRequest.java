package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record RagDocumentBatchRequest(
        String operation,
        List<Long> documentIds
) {
}

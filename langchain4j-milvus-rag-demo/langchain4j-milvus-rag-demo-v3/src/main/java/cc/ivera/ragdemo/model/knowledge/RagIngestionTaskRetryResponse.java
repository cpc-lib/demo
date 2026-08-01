package cc.ivera.ragdemo.model.knowledge;

import cc.ivera.ragdemo.domain.rag.RagIngestionTask;

public record RagIngestionTaskRetryResponse(
        RagIngestionTask task,
        boolean published
) {
}

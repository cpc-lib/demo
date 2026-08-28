package cc.ivera.ragdemo.model.knowledge;

import cc.ivera.ragdemo.domain.rag.RagDocumentVersion;

public record RagDocumentVersionDownload(
        RagDocumentVersion version,
        byte[] bytes
) {
}

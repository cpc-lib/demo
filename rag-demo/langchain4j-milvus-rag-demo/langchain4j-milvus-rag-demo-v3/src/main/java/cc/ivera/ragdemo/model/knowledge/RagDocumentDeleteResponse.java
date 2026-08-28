package cc.ivera.ragdemo.model.knowledge;

import cc.ivera.ragdemo.domain.rag.RagDocument;

public record RagDocumentDeleteResponse(
        RagDocument document,
        int deletedVectorCount,
        int deletedObjectCount,
        int deletedChunkRows
) {
}

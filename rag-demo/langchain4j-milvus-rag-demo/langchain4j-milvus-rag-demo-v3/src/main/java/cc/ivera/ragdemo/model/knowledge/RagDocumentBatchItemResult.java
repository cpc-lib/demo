package cc.ivera.ragdemo.model.knowledge;

public record RagDocumentBatchItemResult(
        Long documentId,
        boolean success,
        String message
) {
}

package cc.ivera.ragdemo.model.knowledge;

public record RagDocumentVersionDiffLine(
        String type,
        Integer leftLineNo,
        Integer rightLineNo,
        String text
) {
}

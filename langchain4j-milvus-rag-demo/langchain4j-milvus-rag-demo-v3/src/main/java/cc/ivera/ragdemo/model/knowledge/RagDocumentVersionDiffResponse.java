package cc.ivera.ragdemo.model.knowledge;

import java.util.List;

public record RagDocumentVersionDiffResponse(
        Long documentId,
        Integer leftVersionNo,
        Integer rightVersionNo,
        boolean identical,
        boolean truncated,
        int addedLines,
        int deletedLines,
        int unchangedLines,
        List<RagDocumentVersionDiffLine> lines
) {
}

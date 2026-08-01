package cc.ivera.ragdemo.model.knowledge;

import jakarta.validation.constraints.NotBlank;

public record RagTextDocumentIngestRequest(
        @NotBlank String text,
        String fileName
) {
}

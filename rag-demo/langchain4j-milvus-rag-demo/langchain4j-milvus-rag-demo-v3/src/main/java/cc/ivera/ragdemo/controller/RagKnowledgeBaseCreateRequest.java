package cc.ivera.ragdemo.controller;

import jakarta.validation.constraints.NotBlank;

public record RagKnowledgeBaseCreateRequest(
        Long tenantId,
        @NotBlank String kbCode,
        @NotBlank String name,
        String description
) {
}

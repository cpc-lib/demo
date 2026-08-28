package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagAgentPromptDto(
        Long id,
        Long tenantId,
        String promptName,
        String promptContent,
        Integer version,
        Integer status,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

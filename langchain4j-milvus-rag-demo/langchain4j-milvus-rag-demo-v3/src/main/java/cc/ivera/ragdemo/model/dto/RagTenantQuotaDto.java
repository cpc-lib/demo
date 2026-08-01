package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagTenantQuotaDto(
        Long id,
        Long maxDocuments,
        Long maxStorageBytes,
        Long maxFileBytes,
        Long dailyOcrLimit,
        Long dailyEmbeddingTokens,
        Long maxConcurrentIngestionTasks,
        Long dailyQueryLimit,
        Long monthlyBudgetCents,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

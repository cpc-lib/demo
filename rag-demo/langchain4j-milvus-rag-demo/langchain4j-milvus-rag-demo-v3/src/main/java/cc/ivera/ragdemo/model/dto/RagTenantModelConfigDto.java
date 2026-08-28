package cc.ivera.ragdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagTenantModelConfigDto(
        Long id,
        String provider,
        String modelType,
        String modelName,
        String baseUrl,
        BigDecimal temperature,
        Integer dimension,
        String imageSize,
        String imageQuality,
        Integer pollIntervalMillis,
        Integer rateLimitQps,
        Long monthlyBudgetCents,
        Boolean apiKeyConfigured,
        Integer timeoutSeconds,
        Integer maxRetries,
        Integer maxTokens,
        BigDecimal frequencyPenalty,
        BigDecimal presencePenalty,
        BigDecimal topP,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

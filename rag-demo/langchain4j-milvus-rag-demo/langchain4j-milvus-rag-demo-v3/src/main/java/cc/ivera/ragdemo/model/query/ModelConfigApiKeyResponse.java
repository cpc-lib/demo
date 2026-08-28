package cc.ivera.ragdemo.model.query;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModelConfigApiKeyResponse(
        Long id,
        String modelType,
        Boolean enabled,
        Boolean apiKeyConfigured,
        String apiKeySecretRef
) {
}

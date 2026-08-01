package cc.ivera.ragdemo.model.query;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RagApiError(
        String code,
        String message,
        Map<String, Object> details
) {
}

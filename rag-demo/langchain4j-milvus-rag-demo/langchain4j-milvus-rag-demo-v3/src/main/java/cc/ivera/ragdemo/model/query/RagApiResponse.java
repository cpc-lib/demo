package cc.ivera.ragdemo.model.query;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagApiResponse<T>(
        boolean ok,
        String traceId,
        T data,
        RagApiError error
) {

    public static <T> RagApiResponse<T> ok(String traceId, T data) {
        return new RagApiResponse<>(true, traceId, data, null);
    }

    public static <T> RagApiResponse<T> ok(T data) {
        return ok(newTraceId(), data);
    }

    public static RagApiResponse<Void> fail(String traceId, String code, String message, Map<String, Object> details) {
        return new RagApiResponse<>(false, traceId, null, new RagApiError(code, message, details));
    }

    public static String newTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }
}

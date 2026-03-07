package cc.ivera.ragdemo.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        boolean ok,
        int code,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, Object> details
) {

    public ApiError {
        details = details == null ? Collections.emptyMap() : details;
        timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    public static ApiError of(int code, String message, String path) {
        return new ApiError(
                false,
                code,
                message,
                path,
                LocalDateTime.now(),
                Collections.emptyMap()
        );
    }

    public static ApiError of(int code, String message, String path, Map<String, Object> details) {
        return new ApiError(
                false,
                code,
                message,
                path,
                LocalDateTime.now(),
                details
        );
    }

    public static ApiError badRequest(String message, String path) {
        return of(400, message, path);
    }

    public static ApiError badRequest(String message, String path, Map<String, Object> details) {
        return of(400, message, path, details);
    }

    public static ApiError unauthorized(String message, String path) {
        return of(401, message, path);
    }

    public static ApiError forbidden(String message, String path) {
        return of(403, message, path);
    }

    public static ApiError notFound(String message, String path) {
        return of(404, message, path);
    }

    public static ApiError internalError(String message, String path) {
        return of(500, message, path);
    }

    public static ApiError internalError(String message, String path, Map<String, Object> details) {
        return of(500, message, path, details);
    }
}
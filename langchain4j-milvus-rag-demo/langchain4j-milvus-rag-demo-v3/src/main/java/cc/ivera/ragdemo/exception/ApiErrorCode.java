package cc.ivera.ragdemo.exception;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    BIZ_ERROR(HttpStatus.BAD_REQUEST, "Business rule violation"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed"),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "Missing required request parameter"),
    MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "Malformed request body"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not allowed"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),
    RESOURCE_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "Resource exhausted, please try again later"),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "External service call failed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String defaultMessage;

    ApiErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return name();
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}

package cc.ivera.ragdemo.exception;

import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.util.TraceUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<RagApiResponse<Void>> handleBizException(BizException ex, HttpServletRequest request) {
        HttpStatus status = httpStatus(ex.getCode());
        return error(status, ApiErrorCode.BIZ_ERROR, ex.getMessage(), request.getRequestURI(), Map.of("legacyCode", ex.getCode()));
    }

    @ExceptionHandler(MissingTenantContextException.class)
    public ResponseEntity<RagApiResponse<Void>> handleMissingTenantContext(MissingTenantContextException ex,
                                                                           HttpServletRequest request) {
        return error(ApiErrorCode.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<RagApiResponse<Void>> handleExternalServiceException(ExternalServiceException ex,
                                                                               HttpServletRequest request) {
        log.warn("External service error: service={}, message={}", ex.getServiceName(), ex.getMessage());
        Map<String, Object> details = new LinkedHashMap<>();
        if (ex.getServiceName() != null) {
            details.put("service", ex.getServiceName());
        }
        if (ex.getHttpStatusCode() > 0) {
            details.put("httpStatusCode", ex.getHttpStatusCode());
        }
        return error(ApiErrorCode.EXTERNAL_SERVICE_ERROR, ex.getMessage(), request.getRequestURI(), details);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<RagApiResponse<Void>> handleRateLimitException(RateLimitException ex,
                                                                         HttpServletRequest request) {
        log.warn("Rate limit exceeded: type={}, retryAfter={}s, message={}",
                ex.getLimitType(), ex.getRetryAfterSeconds(), ex.getMessage());
        Map<String, Object> details = new LinkedHashMap<>();
        if (ex.getRetryAfterSeconds() > 0) {
            details.put("retryAfter", ex.getRetryAfterSeconds());
        }
        if (ex.getLimitType() != null) {
            details.put("limitType", ex.getLimitType());
        }
        return error(ApiErrorCode.RATE_LIMITED, ex.getMessage(), request.getRequestURI(), details);
    }

    @ExceptionHandler(ResourceExhaustedException.class)
    public ResponseEntity<RagApiResponse<Void>> handleResourceExhaustedException(ResourceExhaustedException ex,
                                                                                 HttpServletRequest request) {
        log.error("Resource exhausted: type={}, retryAfter={}s, message={}",
                ex.getResourceType(), ex.getRetryAfterSeconds(), ex.getMessage());
        Map<String, Object> details = new LinkedHashMap<>();
        if (ex.getResourceType() != null) {
            details.put("resourceType", ex.getResourceType());
        }
        if (ex.getRetryAfterSeconds() > 0) {
            details.put("retryAfter", ex.getRetryAfterSeconds());
        }
        return error(ApiErrorCode.RESOURCE_EXHAUSTED, ex.getMessage(), request.getRequestURI(), details);
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<RagApiResponse<Void>> handleTenantAccessDenied(TenantAccessDeniedException ex,
                                                                         HttpServletRequest request) {
        return error(ApiErrorCode.FORBIDDEN, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<RagApiResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        return error(ApiErrorCode.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RagApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex,
                                                                          HttpServletRequest request) {
        Map<String, Object> details = Map.of(
                "violations",
                ex.getConstraintViolations()
                        .stream()
                        .map(v -> Map.of(
                                "property", v.getPropertyPath().toString(),
                                "message", v.getMessage()
                        ))
                        .collect(Collectors.toList())
        );
        return error(ApiErrorCode.VALIDATION_ERROR, "Request parameter validation failed", request.getRequestURI(), details);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RagApiResponse<Void>> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        return error(ApiErrorCode.NOT_FOUND, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RagApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        if (isExternalServiceError(ex)) {
            log.warn("Uncaught external service error: exception={}, message={}", ex.getClass().getSimpleName(), safeMessage(ex));
            return error(
                    ApiErrorCode.EXTERNAL_SERVICE_ERROR,
                    "External service call failed: " + safeMessage(ex),
                    request.getRequestURI(),
                    Map.of("exception", ex.getClass().getSimpleName(), "message", safeMessage(ex))
            );
        }
        log.error("Unexpected exception occurred: uri={}, exception={}, message={}",
                request.getRequestURI(), ex.getClass().getSimpleName(), safeMessage(ex), ex);
        return error(
                ApiErrorCode.INTERNAL_ERROR,
                ApiErrorCode.INTERNAL_ERROR.defaultMessage(),
                request.getRequestURI(),
                Map.of("exception", ex.getClass().getSimpleName(), "message", safeMessage(ex))
        );
    }

    private boolean isExternalServiceError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String name = current.getClass().getName();
            if (name.contains("OpenAi") || name.contains("Milvus") || name.contains("Redis")
                    || name.contains("Rabbit") || name.contains("Elasticsearch") || name.contains("Minio")
                    || name.contains("io.grpc")
                    || name.contains("HttpException") || name.contains("TimeoutException")
                    || (current.getMessage() != null && (
                            current.getMessage().contains("API key")
                                    || current.getMessage().contains("connection refused")
                                    || current.getMessage().contains("timeout")
                                    || current.getMessage().contains("DEADLINE_EXCEEDED")
                                    || current.getMessage().contains("UNAVAILABLE")
                                    || current.getMessage().contains("ConnectException")
                    ))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, Object> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return errorObject(
                ApiErrorCode.VALIDATION_ERROR,
                "Request body validation failed",
                path(request),
                Map.of("fieldErrors", fieldErrors)
        );
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                         HttpHeaders headers,
                                                                        HttpStatusCode status,
                                                                        WebRequest request) {
        return errorObject(
                ApiErrorCode.MISSING_PARAMETER,
                "Missing required request parameter: " + ex.getParameterName(),
                path(request),
                Map.of("parameter", ex.getParameterName(), "parameterType", ex.getParameterType())
        );
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        return errorObject(ApiErrorCode.PAYLOAD_TOO_LARGE, ApiErrorCode.PAYLOAD_TOO_LARGE.defaultMessage(), path(request), Map.of());
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
        return errorObject(ApiErrorCode.NOT_FOUND, "Endpoint not found", path(request), Map.of("httpMethod", ex.getHttpMethod()));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        return errorObject(
                ApiErrorCode.METHOD_NOT_ALLOWED,
                ApiErrorCode.METHOD_NOT_ALLOWED.defaultMessage(),
                path(request),
                Map.of("method", ex.getMethod(), "supportedMethods", ex.getSupportedMethods() == null ? new String[0] : ex.getSupportedMethods())
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers,
                                                                     HttpStatusCode status,
                                                                     WebRequest request) {
        return errorObject(
                ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
                ApiErrorCode.UNSUPPORTED_MEDIA_TYPE.defaultMessage(),
                path(request),
                Map.of("contentType", ex.getContentType() == null ? "" : ex.getContentType().toString())
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return errorObject(
                ApiErrorCode.MESSAGE_NOT_READABLE,
                ApiErrorCode.MESSAGE_NOT_READABLE.defaultMessage(),
                path(request),
                Map.of("message", safeMessage(ex))
        );
    }

    private ResponseEntity<RagApiResponse<Void>> error(ApiErrorCode errorCode,
                                                       String message,
                                                       String path,
                                                       Map<String, Object> details) {
        return error(errorCode.status(), errorCode, message, path, details);
    }

    private ResponseEntity<RagApiResponse<Void>> error(HttpStatus status,
                                                       ApiErrorCode errorCode,
                                                       String message,
                                                       String path,
                                                       Map<String, Object> details) {
        return ResponseEntity.status(status).body(failure(status, errorCode, message, path, details));
    }

    private ResponseEntity<Object> errorObject(ApiErrorCode errorCode,
                                               String message,
                                               String path,
                                               Map<String, Object> details) {
        return ResponseEntity.status(errorCode.status()).body(failure(errorCode.status(), errorCode, message, path, details));
    }

    private RagApiResponse<Void> failure(HttpStatus status,
                                         ApiErrorCode errorCode,
                                         String message,
                                         String path,
                                         Map<String, Object> details) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("status", status.value());
        merged.put("path", path);
        merged.put("timestamp", OffsetDateTime.now().toString());
        if (details != null) {
            merged.putAll(details);
        }
        return RagApiResponse.fail(TraceUtils.currentTraceId(), errorCode.code(), message, merged);
    }

    private HttpStatus httpStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        return status == null ? HttpStatus.BAD_REQUEST : status;
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    private String safeMessage(Throwable ex) {
        // Walk the cause chain to find a non-empty message. Some SDK exceptions (e.g. Milvus
        // SDK 2.3.x MilvusClientException) carry a null/empty top-level message while the actual
        // error text is nested in the cause.
        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
            current = current.getCause();
        }
        return "";
    }
}

package cc.ivera.ragdemo.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiError> handleBizException(BizException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        ex.getCode(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        400,
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
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

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        400,
                        "请求参数校验失败",
                        request.getRequestURI(),
                        details
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiError.of(
                        413,
                        "上传文件过大",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        500,
                        "服务器内部异常: " + ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {

        Map<String, Object> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiError body = ApiError.of(
                400,
                "请求体参数校验失败",
                request.getDescription(false).replace("uri=", ""),
                Map.of("fieldErrors", fieldErrors)
        );

        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {

        ApiError body = ApiError.of(
                400,
                "缺少必要请求参数: " + ex.getParameterName(),
                request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.badRequest().body(body);
    }
}
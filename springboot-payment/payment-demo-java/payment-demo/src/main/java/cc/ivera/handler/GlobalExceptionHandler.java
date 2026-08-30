package cc.ivera.handler;

import cc.ivera.exception.BizException;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.exception.UnauthorizedException;
import cc.ivera.vo.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<R<Map<String, Object>>> handleUnauthorized(UnauthorizedException ex) {
        R<Map<String, Object>> body = R.error().setCode(HttpStatus.UNAUTHORIZED.value()).setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<R<Map<String, Object>>> handleForbidden(ForbiddenException ex) {
        R<Map<String, Object>> body = R.error().setCode(HttpStatus.FORBIDDEN.value()).setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(BizException.class)
    public R<Map<String, Object>> handleBizException(BizException ex) {
        return R.error().setMessage(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return R.<Map<String, Object>>error(message);
    }

    @ExceptionHandler(BindException.class)
    public R<Map<String, Object>> handleBindException(BindException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return R.<Map<String, Object>>error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return R.<Map<String, Object>>error(message);
    }

    @ExceptionHandler(Exception.class)
    public R<Map<String, Object>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return R.error().setMessage(ex.getMessage() == null ? "系统异常" : ex.getMessage());
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + "：" + fieldError.getDefaultMessage();
    }
}

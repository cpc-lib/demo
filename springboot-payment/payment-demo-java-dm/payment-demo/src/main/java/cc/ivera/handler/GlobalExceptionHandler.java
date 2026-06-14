package cc.ivera.handler;

import cc.ivera.exception.BizException;
import cc.ivera.vo.R;
import lombok.extern.slf4j.Slf4j;
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

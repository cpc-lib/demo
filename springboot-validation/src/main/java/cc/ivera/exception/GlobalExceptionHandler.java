package cc.ivera.exception;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import cc.ivera.vo.AjaxResult;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final static Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult validationErrorHandler(MethodArgumentNotValidException ex) {
        List<String> errorInformation = ex.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage).collect(Collectors.toList());
        return AjaxResult.error("失败", errorInformation);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult validationErrorHandlerOther(ConstraintViolationException ex) {
        List<String> errorInformation = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        return AjaxResult.error("失败", errorInformation);
    }

    @ExceptionHandler
    public AjaxResult validationErrorHandlerDefault(Exception e) {
        return AjaxResult.error(e.getMessage());
    }
}


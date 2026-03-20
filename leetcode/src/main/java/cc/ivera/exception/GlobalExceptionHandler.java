package cc.ivera.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import cc.ivera.model.vo.AjaxResult;
import cc.ivera.model.vo.ErrorResult;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity handlerBusinessException(BusinessException be) {
        be.printStackTrace();
        ErrorResult errorResult = be.getErrorResult();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity handlerException(Exception be) {
        be.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResult.error());
    }

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


//    @ExceptionHandler
//    public AjaxResult validationErrorHandlerDefault(Exception e) {
//        return AjaxResult.error(e.getMessage());
//    }
}

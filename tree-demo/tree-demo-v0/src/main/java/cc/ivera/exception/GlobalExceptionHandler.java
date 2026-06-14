
package cc.ivera.exception;

import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handle(BusinessException e) {
        return e.getMessage();
    }
}

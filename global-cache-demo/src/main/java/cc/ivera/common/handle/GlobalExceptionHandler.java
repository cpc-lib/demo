package cc.ivera.common.handle;

import cc.ivera.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ResponseBody
    @ExceptionHandler(value = BusinessException.class)
    public Object errorHandler(BusinessException ex, HttpServletRequest request) {
        Object jsonMap = request.getAttribute("resultData");
        return jsonMap;
    }
}
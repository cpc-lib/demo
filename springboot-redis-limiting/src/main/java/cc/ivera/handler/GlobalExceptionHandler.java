package cc.ivera.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import cc.ivera.domain.vo.Result;
import cc.ivera.exception.ServiceRuntimeException;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceRuntimeException.class)
    public Result handleServiceException(ServiceRuntimeException e, HttpServletRequest request) {
        return Result.fail(1001, e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e, HttpServletRequest request) {
        return Result.fail(500, "系统异常");
    }

}

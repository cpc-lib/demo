package cc.ivera.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import cc.ivera.dto.Response;

/**
 * 全局异常处理器
 *
 * @author hweiyu
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理Exceptionn异常
     *
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Response handleException(Exception e) {
        print(e);
        String message = e.getMessage();
        if (null == message || "".equals(message)) {
            message = e.getClass().getSimpleName();
        }
        return Response.error(message);
    }

    private void print(Exception e) {
        log.error(" Exception : {}", e.getClass().getSimpleName(), e);
    }
}

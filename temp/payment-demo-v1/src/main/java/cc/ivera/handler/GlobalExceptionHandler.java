package cc.ivera.handler;

import cc.ivera.exception.BizException;
import cc.ivera.vo.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R handleBizException(BizException ex) {
        return R.error().setMessage(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R handleException(Exception ex) {
        log.error("系统异常", ex);
        return R.error().setMessage(ex.getMessage() == null ? "系统异常" : ex.getMessage());
    }
}

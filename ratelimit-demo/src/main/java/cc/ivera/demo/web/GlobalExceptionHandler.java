package cc.ivera.demo.web;

import cc.ivera.ratelimit.core.exception.RateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleRateLimit(RateLimitException ex) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("code", 429);
        m.put("message", ex.getMessage());
        return m;
    }
}

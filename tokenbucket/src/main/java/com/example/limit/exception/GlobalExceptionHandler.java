package com.example.limit.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获所有异常
     */
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception e) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 500);
        res.put("msg", e.getMessage());
        return res;
    }

    /**
     * 专门捕获限流异常
     */
    @ExceptionHandler(RateLimitException.class)
    public Map<String, Object> handleRuntime(RateLimitException e) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 429);  // HTTP 429: Too Many Requests
        res.put("msg", e.getMessage());
        return res;
    }
}

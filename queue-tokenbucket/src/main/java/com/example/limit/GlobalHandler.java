package com.example.limit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：
 *
 * 统一捕获 LimitException，并返回 HTTP 429（Too Many Requests）。
 */
@RestControllerAdvice
public class GlobalHandler {

    @ExceptionHandler(LimitException.class)
    public ResponseEntity<String> handleLimit(LimitException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
    }
}

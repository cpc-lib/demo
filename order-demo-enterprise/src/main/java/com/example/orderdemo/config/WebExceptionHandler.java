package com.example.orderdemo.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Err handleBadRequest(IllegalArgumentException e) {
        return new Err("BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Err handleConflict(IllegalStateException e) {
        return new Err("CONFLICT", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Err handleServer(Exception e) {
        return new Err("INTERNAL_ERROR", e.getMessage());
    }

    @Data
    @AllArgsConstructor
    static class Err {
        private String code;
        private String message;
    }
}

package com.example.vocab.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(IllegalArgumentException e) {
        return Map.of("code", "BAD_REQUEST", "message", e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> validation(Exception e) {
        return Map.of("code", "VALIDATION_ERROR", "message", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> server(Exception e) {
        return Map.of("code", "SERVER_ERROR", "message", e.getMessage());
    }
}

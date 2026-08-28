package com.example.idgen.controller;

import com.example.idgen.service.IdGenUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdGenUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handle(IdGenUnavailableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "IDGEN_UNAVAILABLE");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}

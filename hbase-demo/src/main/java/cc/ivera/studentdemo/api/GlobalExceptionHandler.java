package cc.ivera.studentdemo.api;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Err> handle(IllegalStateException e) {
    String code = e.getMessage() == null ? "error" : e.getMessage();
    HttpStatus st = "not_found".equals(code) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(st).body(new Err(code, e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Err> handleAny(Exception e) {
    return ResponseEntity.status(500).body(new Err("internal_error", e.toString()));
  }

  @Data
  public static class Err {
    private final String code;
    private final String message;
  }
}

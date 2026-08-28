package cc.ivera.userdemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleValid(MethodArgumentNotValidException ex) {
    Map<String, Object> m = new HashMap<>();
    m.put("code", "BAD_REQUEST");
    m.put("message", ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    return m;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleBad(IllegalArgumentException ex) {
    return Map.of("code", "BAD_REQUEST", "message", ex.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public Map<String, Object> handleConflict(IllegalStateException ex) {
    return Map.of("code", "CONFLICT", "message", ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Map<String, Object> handleOther(Exception ex) {
    log.error("internal error", ex);
    return Map.of("code", "INTERNAL_ERROR", "message", ex.getMessage());
  }
}

package com.example.fencing.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(LockBusyException.class)
    public ResponseEntity<ProblemDetail> lockBusy(LockBusyException exception) {
        return problem(HttpStatus.LOCKED, "LOCK_BUSY", exception.getMessage());
    }

    @ExceptionHandler(LockInterruptedException.class)
    public ResponseEntity<ProblemDetail> interrupted(LockInterruptedException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "LOCK_INTERRUPTED", exception.getMessage());
    }

    @ExceptionHandler(LockLostException.class)
    public ResponseEntity<ProblemDetail> lost(LockLostException exception) {
        return problem(HttpStatus.CONFLICT, "LOCK_LOST", exception.getMessage());
    }

    @ExceptionHandler(StaleFenceException.class)
    public ResponseEntity<ProblemDetail> stale(StaleFenceException exception) {
        return problem(HttpStatus.CONFLICT, "STALE_FENCE", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> duplicate(IdempotencyConflictException exception) {
        return problem(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(SeckillStockOutException.class)
    public ResponseEntity<ProblemDetail> soldOut(SeckillStockOutException exception) {
        return problem(HttpStatus.GONE, "STOCK_OUT", exception.getMessage());
    }

    @ExceptionHandler(AlreadyPurchasedException.class)
    public ResponseEntity<ProblemDetail> alreadyPurchased(AlreadyPurchasedException exception) {
        return problem(HttpStatus.CONFLICT, "ALREADY_PURCHASED", exception.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> rateLimited(RateLimitExceededException exception) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", exception.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> auth(AuthException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "AUTH_FAILED", exception.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(ProductNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> invalid(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid request parameters");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(code);
        detail.setProperty("errorCode", code);
        return ResponseEntity.status(status).body(detail);
    }
}

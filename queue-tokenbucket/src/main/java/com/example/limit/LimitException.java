package com.example.limit;

/**
 * 自定义限流异常。
 *
 * 所有被限流 / 排队超时 / 令牌桶拒绝的请求，
 * 都会抛出该异常，统一在 GlobalExceptionHandler 中处理。
 */
public class LimitException extends RuntimeException {

    public LimitException(String message) {
        super(message);
    }
}

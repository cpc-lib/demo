package cc.ivera.ragdemo.exception;

/**
 * 限流异常
 * 用于表示请求频率超过限制
 */
public class RateLimitException extends RuntimeException {

    private final long retryAfterSeconds;
    private final String limitType;

    public RateLimitException(String message) {
        super(message);
        this.retryAfterSeconds = 0;
        this.limitType = null;
    }

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.limitType = null;
    }

    public RateLimitException(String message, long retryAfterSeconds, String limitType) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.limitType = limitType;
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = 0;
        this.limitType = null;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getLimitType() {
        return limitType;
    }
}
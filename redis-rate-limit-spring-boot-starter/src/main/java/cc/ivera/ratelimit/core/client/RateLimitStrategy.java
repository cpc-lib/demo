package cc.ivera.ratelimit.core.client;

public enum RateLimitStrategy {
    FIXED_WINDOW,
    SLIDING_WINDOW,
    TOKEN_BUCKET
}

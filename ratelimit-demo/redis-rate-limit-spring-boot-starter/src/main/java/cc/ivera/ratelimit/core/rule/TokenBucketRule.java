package cc.ivera.ratelimit.core.rule;

import cc.ivera.ratelimit.core.client.RateLimitStrategy;

public class TokenBucketRule implements RateLimitRule {
    private final int capacity;
    private final double refillPerSecond;
    private final int permits;

    public TokenBucketRule(int capacity, double refillPerSecond, int permits) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.permits = permits;
    }

    public int getCapacity() { return capacity; }
    public double getRefillPerSecond() { return refillPerSecond; }

    @Override public int permits() { return permits; }
    @Override public RateLimitStrategy strategy() { return RateLimitStrategy.TOKEN_BUCKET; }
}

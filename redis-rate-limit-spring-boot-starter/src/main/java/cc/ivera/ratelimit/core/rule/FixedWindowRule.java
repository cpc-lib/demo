package cc.ivera.ratelimit.core.rule;

import cc.ivera.ratelimit.core.client.RateLimitStrategy;

public class FixedWindowRule implements RateLimitRule {
    private final int limit;
    private final long windowMillis;
    private final int permits;

    public FixedWindowRule(int limit, long windowMillis, int permits) {
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.permits = permits;
    }

    public int getLimit() { return limit; }
    public long getWindowMillis() { return windowMillis; }

    @Override public int permits() { return permits; }
    @Override public RateLimitStrategy strategy() { return RateLimitStrategy.FIXED_WINDOW; }
}

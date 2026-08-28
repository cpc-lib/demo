package cc.ivera.ratelimit.core.client;

public class RateLimitResult {
    private final boolean allowed;
    private final long remaining;
    private final long resetMillis;

    public RateLimitResult(boolean allowed, long remaining, long resetMillis) {
        this.allowed = allowed;
        this.remaining = remaining;
        this.resetMillis = resetMillis;
    }

    public boolean isAllowed() { return allowed; }
    public long getRemaining() { return remaining; }
    public long getResetMillis() { return resetMillis; }
}

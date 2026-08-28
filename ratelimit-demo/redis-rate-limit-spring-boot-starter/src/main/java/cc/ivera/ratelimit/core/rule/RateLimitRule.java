package cc.ivera.ratelimit.core.rule;

import cc.ivera.ratelimit.core.client.RateLimitStrategy;

public interface RateLimitRule {
    RateLimitStrategy strategy();
    int permits();
}

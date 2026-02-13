package cc.ivera.demo.web;

import cc.ivera.ratelimit.core.annotation.RateLimit;
import cc.ivera.ratelimit.core.client.RateLimitResult;
import cc.ivera.ratelimit.core.client.RateLimitStrategy;
import cc.ivera.ratelimit.core.client.RateLimiterClient;
import cc.ivera.ratelimit.core.rule.RateLimitRule;
import cc.ivera.ratelimit.core.rule.TokenBucketRule;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class RateLimitDemoController {

    private final RateLimiterClient client;

    public RateLimitDemoController(RateLimiterClient client) {
        this.client = client;
    }

    // Fixed window: 5 req / 10s per (ip + path)
    @GetMapping("/fixed")
    @RateLimit(strategy = RateLimitStrategy.FIXED_WINDOW,
            key = "'fw:' + #request.getRemoteAddr() + ':' + #request.getRequestURI()",
            limit = 5, windowSeconds = 10)
    public Map<String, Object> fixed(HttpServletRequest request) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("ok", true);
        m.put("msg", "fixed ok");
        return m;
    }

    // Sliding window: 5 req / 10s per uid (query param)
    @GetMapping("/sliding")
    @RateLimit(strategy = RateLimitStrategy.SLIDING_WINDOW,
            key = "'sw:' + #uid",
            limit = 5, windowSeconds = 10)
    public Map<String, Object> sliding(@RequestParam("uid") String uid) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("ok", true);
        m.put("msg", "sliding ok uid=" + uid);
        return m;
    }

    // Token bucket: capacity 10, refill 2/s, per uid
    @PostMapping("/bucket")
    public Map<String, Object> bucket(@RequestParam("uid") String uid,
                                      @RequestParam(value = "permits", defaultValue = "1") int permits) {
        RateLimitRule rule = new TokenBucketRule(10, 2.0, permits);
        RateLimitResult r = client.tryAcquire("tb:" + uid, rule);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("allowed", r.isAllowed());
        m.put("remaining", r.getRemaining());
        m.put("resetMillis", r.getResetMillis());
        return m;
    }
}

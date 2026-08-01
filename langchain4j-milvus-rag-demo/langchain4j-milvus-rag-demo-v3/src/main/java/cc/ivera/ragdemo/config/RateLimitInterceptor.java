package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.annotation.RateLimit;
import cc.ivera.ragdemo.exception.RateLimitException;
import cc.ivera.ragdemo.service.ratelimit.SlidingWindowRateLimiter;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流拦截器
 * 拦截带有 @RateLimit 注解的接口，执行滑动窗口限流
 */
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final SlidingWindowRateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只处理 Controller 方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;
        }

        // 生成限流 key
        String key = buildKey(request, rateLimit);
        int max = rateLimit.max();
        int windowSeconds = rateLimit.windowSeconds();

        try {
            long currentCount = rateLimiter.tryAcquire(key, max, windowSeconds);
            log.debug("Rate limit check passed: key={}, count={}/{}, window={}s",
                    key, currentCount, max, windowSeconds);
            return true;
        } catch (RateLimitException e) {
            log.warn("Rate limit exceeded for key={}: max={}, window={}s, retryAfter={}s",
                    key, max, windowSeconds, e.getRetryAfterSeconds());
            throw e;
        }
    }

    /**
     * 根据限流维度生成限流 key
     */
    private String buildKey(HttpServletRequest request, RateLimit rateLimit) {
        String uri = request.getRequestURI();
        String ip = getClientIp(request);
        String userId = request.getHeader("X-User-Id");
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);

        return switch (rateLimit.key()) {
            case IP -> "rate:ip:" + ip;
            case USER -> "rate:user:" + userId;
            case TENANT -> "rate:tenant:" + tenantId;
            case IP_URI -> "rate:ip_uri:" + ip + ":" + uri;
            case TENANT_URI -> "rate:tenant_uri:" + tenantId + ":" + uri;
        };
    }

    /**
     * 获取客户端真实 IP（支持代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

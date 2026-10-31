package com.example.scanlogin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Fixed-window throttle for the password login endpoints, keyed by client IP.
 * Blocks credential stuffing before any credential or captcha work happens.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final DefaultRedisScript<Long> INCREMENT_WINDOW = new DefaultRedisScript<>(
            "local hits = redis.call('INCR', KEYS[1]) "
            + "if hits == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
            + "return hits", Long.class);

    private final StringRedisTemplate redis;
    private final int maxRequests;
    private final int windowSeconds;

    public LoginRateLimitFilter(StringRedisTemplate redis,
                                @Value("${app.rate-limit.login.max-requests:5}") int maxRequests,
                                @Value("${app.rate-limit.login.window-seconds:60}") int windowSeconds) {
        this.redis = redis;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean loginPath = request.getRequestURI().equals("/api/auth/login")
                || request.getRequestURI().equals("/api/mobile/login");
        return !loginPath || !HttpMethod.POST.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = clientIp(request);
        Long hits = redis.execute(INCREMENT_WINDOW,
                List.of("auth:ratelimit:login:" + ip), Integer.toString(windowSeconds));
        if (hits != null && hits > maxRequests) {
            log.warn("Login rate limited, ip={}, hits={}", ip, hits);
            response.setStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Integer.toString(windowSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"请求过于频繁，请稍后再试\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Honors the dev proxy header; production gateways must send X-Forwarded-For. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

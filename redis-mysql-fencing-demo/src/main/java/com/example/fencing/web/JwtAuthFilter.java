package com.example.fencing.web;

import com.example.fencing.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器：
 *   放行 /api/auth/login 和 /actuator/** 以外的所有请求，要求 Authorization: Bearer <token>。
 *   验签成功后把 userId 注入 request attribute "com.example.fencing.userId"。
 *
 * 注意：这是 demo 级认证——不做黑名单、不做刷新 token、不验证用户是否仍被启用。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /** Controller 从 request 读 userId 用的 attribute key。 */
    public static final String USER_ID_ATTR = "com.example.fencing.userId";

    /** 这些路径永远放行（不需要 JWT）。 */
    private static final String[] PUBLIC_PREFIXES = {
            "/api/auth/login",
            "/api/counters",
            "/actuator"
    };

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 判断是否"需要但也允许匿名"——GET 查询类路径，让 Controller 自己决定
        if (isPublic(path)) {
            // 即使是 public 路径，如果请求里带了 token 也验证一下（让 Controller 拿到 userId）
            extractAndInjectIfPresent(request);
            filterChain.doFilter(request, response);
            return;
        }

        // 非 public 路径：必须有合法 JWT
        String token = extractToken(request);
        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header");
            return;
        }
        try {
            String userId = jwt.validateAndGetUserId(token);
            request.setAttribute(USER_ID_ATTR, userId);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void extractAndInjectIfPresent(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            try {
                String userId = jwt.validateAndGetUserId(token);
                request.setAttribute(USER_ID_ATTR, userId);
            } catch (Exception ignored) {
                // 匿名路径：token 无效就忽略，不拦截
            }
        }
    }

    private boolean isPublic(String path) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}

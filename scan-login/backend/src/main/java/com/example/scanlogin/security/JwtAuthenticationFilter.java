package com.example.scanlogin.security;

import com.example.scanlogin.infrastructure.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Mobile bearer tokens cannot be used to authenticate the browser, and vice versa. */
final class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final AuthCookies cookies;
    private final UserRepository users;

    JwtAuthenticationFilter(JwtService jwt, AuthCookies cookies, UserRepository users) {
        this.jwt = jwt;
        this.cookies = cookies;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean mobile = isMobilePath(path);
        boolean pc = path.equals("/api/auth/me") || path.equals("/api/auth/logout");
        String token = mobile ? bearerToken(request) :
                pc ? cookies.get(request, AuthCookies.PC_ACCESS) : null;

        if (token != null) {
            try {
                JwtService.Identity identity = jwt.verify(token);
                boolean permitted = mobile && "mobile".equals(identity.kind()) ||
                        pc && "pc".equals(identity.kind());
                // Session-version check: kicked users fail here instantly even with
                // an unexpired access token.
                if (permitted && users.tokenVersion(identity.userId()) == identity.tokenVersion()) {
                    String role = mobile ? "ROLE_MOBILE" : "ROLE_PC";
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(identity.userId(), null,
                                    List.of(new SimpleGrantedAuthority(role))));
                }
            } catch (RuntimeException invalid) {
                // Do not expose JWT contents or parse errors to clients/logs.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private static String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()) : null;
    }

    private static boolean isMobilePath(String path) {
        return path.equals("/api/mobile/me") ||
                path.equals("/api/auth/qr/scan") ||
                path.equals("/api/auth/qr/confirm");
    }
}

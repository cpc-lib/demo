package cc.ivera.controller;

import cc.ivera.config.AuthProperties;
import cc.ivera.dto.LoginRequest;
import cc.ivera.dto.PasswordChangeRequest;
import cc.ivera.dto.RegisterRequest;
import cc.ivera.exception.UnauthorizedException;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.AuthService;
import cc.ivera.vo.AuthSession;
import cc.ivera.vo.R;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String REFRESH_COOKIE = "payment_refresh_token";

    private final AuthService authService;

    private final AuthProperties properties;

    public AuthController(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/register")
    public R<AuthSession> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthSession session = authService.register(request);
        writeRefreshCookie(response, session.getRefreshToken(), properties.getRefreshTokenSeconds());
        return R.ok(session);
    }

    @PostMapping("/login")
    public R<AuthSession> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthSession session = authService.login(request);
        writeRefreshCookie(response, session.getRefreshToken(), properties.getRefreshTokenSeconds());
        return R.ok(session);
    }

    @PostMapping("/refresh")
    public R<AuthSession> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String rawRefreshToken,
            @RequestHeader(value = "Origin", required = false) String origin,
            HttpServletResponse response
    ) {
        validateCookieRequestOrigin(origin);
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new UnauthorizedException("刷新令牌不存在");
        }
        AuthSession session = authService.refresh(rawRefreshToken);
        writeRefreshCookie(response, session.getRefreshToken(), properties.getRefreshTokenSeconds());
        return R.ok(session);
    }

    @PostMapping("/logout")
    public R<Void> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String rawRefreshToken,
            @RequestHeader(value = "Origin", required = false) String origin,
            HttpServletResponse response
    ) {
        validateCookieRequestOrigin(origin);
        authService.logout(rawRefreshToken);
        writeRefreshCookie(response, "", 0L);
        return R.ok(null);
    }

    @GetMapping("/me")
    public R<AuthUser> me() {
        return R.ok(AuthContext.requireUser());
    }

    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(AuthContext.requireUser(), request);
        return R.ok(null);
    }

    private void writeRefreshCookie(HttpServletResponse response, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.isSecureCookie())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void validateCookieRequestOrigin(String origin) {
        if (StringUtils.hasText(origin) && !properties.getAllowedOrigins().contains(origin)) {
            throw new ForbiddenException("刷新请求来源不受信任");
        }
    }
}

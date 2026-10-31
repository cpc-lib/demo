package com.example.scanlogin.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Single place for cookie names, scopes and security attributes. */
@Component
public class AuthCookies {
    public static final String QR_BROWSER = "qr_browser";
    public static final String PC_ACCESS = "PC_ACCESS";
    public static final String PC_REFRESH = "PC_REFRESH";
    private final boolean secure;

    public AuthCookies(@Value("${app.qr.secure-cookie:false}") boolean secure) {
        this.secure = secure;
    }

    public String get(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public void bindBrowser(HttpServletResponse response, String value) {
        set(response, QR_BROWSER, value, "/api/auth/qr", Duration.ofMinutes(5));
    }

    /** Access cookie covers /api; refresh cookie is scoped to the auth endpoints that rotate it. */
    public void loginPc(HttpServletResponse response, String accessToken, String refreshToken) {
        set(response, PC_ACCESS, accessToken, "/api", Duration.ofMinutes(30));
        set(response, PC_REFRESH, refreshToken, "/api/auth", Duration.ofDays(7));
    }

    public void logoutPc(HttpServletResponse response) {
        set(response, PC_ACCESS, "", "/api", Duration.ZERO);
        set(response, PC_REFRESH, "", "/api/auth", Duration.ZERO);
    }

    private void set(HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value)
                .httpOnly(true).secure(secure).sameSite("Strict")
                .path(path).maxAge(maxAge).build().toString());
    }
}

package com.example.scanlogin.api;

import com.example.scanlogin.application.CaptchaService;
import com.example.scanlogin.application.QrLoginService;
import com.example.scanlogin.application.RefreshTokenService;
import com.example.scanlogin.application.UserAccountService;
import com.example.scanlogin.security.AuthCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class QrAuthController {
    private final QrLoginService qr;
    private final UserAccountService accounts;
    private final AuthCookies cookies;
    private final CaptchaService captcha;
    private final RefreshTokenService refreshTokens;

    public QrAuthController(QrLoginService qr, UserAccountService accounts, AuthCookies cookies,
                            CaptchaService captcha, RefreshTokenService refreshTokens) {
        this.qr = qr;
        this.accounts = accounts;
        this.cookies = cookies;
        this.captcha = captcha;
        this.refreshTokens = refreshTokens;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrf) {
        return Map.of("token", csrf.getToken());
    }

    @GetMapping("/captcha")
    public CaptchaService.CaptchaChallenge captcha() {
        return captcha.issue();
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody MobileAuthController.LoginRequest body,
                                      HttpServletRequest request, HttpServletResponse response) {
        captcha.verify(body.captchaId(), body.captchaCode());
        UserAccountService.MobileLogin result = accounts.loginPc(body.username(), body.password(), request);
        cookies.loginPc(response, result.accessToken(), result.refreshToken());
        return Map.of("status", "LOGGED_IN", "username", result.username());
    }

    /** Exchanges the long-lived refresh cookie for a fresh pair; rotates the stored token. */
    @PostMapping("/refresh")
    public Map<String, String> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookies.get(request, AuthCookies.PC_REFRESH);
        UserAccountService.MobileLogin result = accounts.refreshPc(refreshToken);
        cookies.loginPc(response, result.accessToken(), result.refreshToken());
        return Map.of("status", "REFRESHED");
    }

    @PostMapping("/qr/create")
    public QrLoginService.CreateResult create(HttpServletRequest request, HttpServletResponse response) {
        // Reuse the browser's existing binding secret: rotating it on every create
        // (page reload, logout refresh, StrictMode double mount, second tab) would
        // invalidate tickets that are still being polled in this browser.
        String binding = cookies.get(request, AuthCookies.QR_BROWSER);
        if (binding == null || binding.isBlank()) binding = qr.randomSecret();
        QrLoginService.CreateResult created = qr.create(binding);
        cookies.bindBrowser(response, binding);
        return created;
    }

    @GetMapping("/qr/status")
    public QrLoginService.StatusResult status(@RequestParam String ticket, HttpServletRequest request) {
        return qr.status(ticket, cookies.get(request, AuthCookies.QR_BROWSER));
    }

    @PostMapping("/qr/scan")
    public QrLoginService.StatusResult scan(@Valid @RequestBody TicketRequest body,
                                             @AuthenticationPrincipal Long userId) {
        return qr.scan(body.ticket(), userId);
    }

    @PostMapping("/qr/confirm")
    public QrLoginService.StatusResult confirm(@Valid @RequestBody TicketRequest body,
                                                @AuthenticationPrincipal Long userId) {
        return qr.confirm(body.ticket(), userId);
    }

    @PostMapping("/qr/consume")
    public Map<String, String> consume(@Valid @RequestBody TicketRequest body,
                                        HttpServletRequest request, HttpServletResponse response) {
        QrLoginService.ConsumeResult result = qr.consume(body.ticket(), cookies.get(request, AuthCookies.QR_BROWSER));
        cookies.loginPc(response, result.accessToken(), result.refreshToken());
        accounts.audit(result.userId(), "QR_PC_LOGIN", request);
        return Map.of("status", "LOGGED_IN");
    }

    @GetMapping("/me")
    public UserAccountService.UserInfo me(@AuthenticationPrincipal Long userId) {
        return accounts.requireActive(userId);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokens.revoke(cookies.get(request, AuthCookies.PC_REFRESH));
        cookies.logoutPc(response);
        return Map.of("status", "LOGGED_OUT");
    }

    public record TicketRequest(@NotBlank String ticket) {}
}

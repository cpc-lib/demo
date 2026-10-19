package com.example.scanlogin.api;

import com.example.scanlogin.application.CaptchaService;
import com.example.scanlogin.application.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile")
public class MobileAuthController {
    private final UserAccountService accounts;
    private final CaptchaService captcha;

    public MobileAuthController(UserAccountService accounts, CaptchaService captcha) {
        this.accounts = accounts;
        this.captcha = captcha;
    }

    @PostMapping("/register")
    public UserAccountService.UserInfo register(@Valid @RequestBody Credentials body) {
        captcha.verify(body.captchaId(), body.captchaCode());
        return accounts.register(body.username(), body.password());
    }

    @PostMapping("/login")
    public UserAccountService.MobileLogin login(@Valid @RequestBody LoginRequest body,
                                                 HttpServletRequest request) {
        captcha.verify(body.captchaId(), body.captchaCode());
        return accounts.loginMobile(body.username(), body.password(), request);
    }

    /** Exchanges a valid mobile refresh token for a fresh access/refresh pair. */
    @PostMapping("/refresh")
    public UserAccountService.MobileLogin refresh(@Valid @RequestBody RefreshRequest body) {
        return accounts.refreshMobile(body.refreshToken());
    }

    @GetMapping("/me")
    public UserAccountService.UserInfo me(@AuthenticationPrincipal Long userId) {
        return accounts.requireActive(userId);
    }

    /** Registration consumes a captcha too, so bots cannot mass-create accounts. */
    public record Credentials(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}") String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String captchaId,
            @NotBlank String captchaCode) {}

    public record LoginRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,40}") String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String captchaId,
            @NotBlank String captchaCode) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}
}

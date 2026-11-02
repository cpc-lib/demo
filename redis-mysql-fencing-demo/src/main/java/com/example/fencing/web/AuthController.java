package com.example.fencing.web;

import com.example.fencing.error.AuthException;
import com.example.fencing.model.AuthLoginRequest;
import com.example.fencing.model.AuthLoginResponse;
import com.example.fencing.service.JwtService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀 Demo 登录端点：硬编码测试账号（不查 DB，不做密码加盐）。
 *
 * 账号: alice/bob/charlie/dave  密码: 123456
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // 硬编码测试账号：username -> userId（两者一致即可，demo 级别）
    private static final Map<String, String> USERS = Map.of(
            "alice",   "alice",
            "bob",     "bob",
            "charlie", "charlie",
            "dave",    "dave"
    );
    private static final String DEFAULT_PASSWORD = "123456";

    private final JwtService jwt;
    private final long expirationMs;

    public AuthController(JwtService jwt,
                          @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.jwt = jwt;
        this.expirationMs = expirationMs;
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@Valid @RequestBody AuthLoginRequest req) {
        String userId = USERS.get(req.username());
        if (userId == null || !DEFAULT_PASSWORD.equals(req.password())) {
            throw new AuthException("Invalid username or password");
        }
        String token = jwt.generate(userId);
        return new AuthLoginResponse(token, userId, expirationMs);
    }
}

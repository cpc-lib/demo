package com.example.vocab.controller.auth;

import com.example.vocab.dto.auth.AuthRequest;
import com.example.vocab.dto.auth.AuthResponse;
import com.example.vocab.dto.auth.ChangePasswordRequest;
import com.example.vocab.dto.auth.RegisterRequest;
import com.example.vocab.security.CurrentUser;
import com.example.vocab.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody @Valid RegisterRequest request) { return authService.register(request); }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) { return authService.login(request); }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.requiredId(), request);
        return Map.of("success", true, "message", "密码修改成功");
    }
}

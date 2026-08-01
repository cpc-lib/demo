package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.security.ChangePasswordRequest;
import cc.ivera.ragdemo.security.LocalAuthService;
import cc.ivera.ragdemo.security.LoginRequest;
import cc.ivera.ragdemo.security.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "Authentication", description = "Local password login and password management")
public class AuthController {

    private final LocalAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with local username and password")
    public RagApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return RagApiResponse.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change current user's password")
    public RagApiResponse<Map<String, Boolean>> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return RagApiResponse.ok(Map.of("changed", true));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user")
    public RagApiResponse<Map<String, Boolean>> logout() {
        authService.logout();
        return RagApiResponse.ok(Map.of("loggedOut", true));
    }
}

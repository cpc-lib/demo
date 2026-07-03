package cc.ivera.gray.admin.controller;

import cc.ivera.gray.admin.security.AuthRequest;
import cc.ivera.gray.admin.security.AuthResponse;
import cc.ivera.gray.admin.security.JwtService;
import cc.ivera.gray.common.ApiResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtService jwtService;
    private final Map<String, DemoUser> users;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
        this.users = new HashMap<>();
        users.put("admin", new DemoUser("admin123", List.of("ADMIN")));
        users.put("release", new DemoUser("release123", List.of("RELEASE_MANAGER")));
        users.put("viewer", new DemoUser("viewer123", List.of("VIEWER")));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody AuthRequest request) {
        DemoUser user = users.get(request.getUsername());
        if (user == null || !user.password().equals(request.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = jwtService.issue(request.getUsername(), user.roles());
        return ApiResponse.ok(new AuthResponse(token, request.getUsername(), user.roles()));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String newPassword = body.get("newPassword");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        DemoUser user = users.get(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + username);
        }
        users.put(username, new DemoUser(newPassword, user.roles()));
        return ApiResponse.ok(null);
    }

    private static class DemoUser {
        private final String password;
        private final List<String> roles;

        DemoUser(String password, List<String> roles) {
            this.password = password;
            this.roles = roles;
        }

        String password() {
            return password;
        }

        List<String> roles() {
            return roles;
        }
    }
}


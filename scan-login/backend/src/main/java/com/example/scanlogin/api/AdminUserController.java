package com.example.scanlogin.api;

import com.example.scanlogin.application.UserAccountService;
import com.example.scanlogin.infrastructure.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Admin console API for the kick feature. Guarded by a shared admin key header
 * instead of user roles; the check is entirely server-side.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    private final UserAccountService accounts;
    private final UserRepository users;
    private final String adminKey;

    public AdminUserController(UserAccountService accounts, UserRepository users,
                               @Value("${app.admin.key:}") String adminKey) {
        this.accounts = accounts;
        this.users = users;
        this.adminKey = adminKey;
    }

    @GetMapping("/users")
    public List<UserRepository.UserSummary> list(HttpServletRequest request) {
        requireAdminKey(request);
        return users.listSummaries();
    }

    @PostMapping("/users/{id}/kick")
    public Map<String, Object> kick(@PathVariable long id, HttpServletRequest request) {
        requireAdminKey(request);
        users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        accounts.kickUser(id);
        return Map.of("status", "KICKED",
                "tokenVersion", users.tokenVersion(id),
                "message", "已踢出：该用户所有端立即下线且无法续期");
    }

    private void requireAdminKey(HttpServletRequest request) {
        if (adminKey == null || adminKey.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "管理密钥未配置");
        String presented = request.getHeader("X-Admin-Key");
        if (!adminKey.equals(presented))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理密钥错误");
    }
}

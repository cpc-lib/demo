package com.example.versionedcachemp.web;

import com.example.versionedcachemp.cache.UserCacheValue;
import com.example.versionedcachemp.service.UserAccountService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST 接口：
 * GET  /api/users/{id}          查询用户（Cache-Aside）
 * POST /api/users/{id}/charge   给用户加钱（写 MySQL + 发 MQ 异步刷新缓存）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userService;

    @GetMapping("/{id}")
    public UserCacheValue getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping("/{id}/charge")
    public UserCacheValue charge(@PathVariable Long id, @RequestBody ChargeRequest req) {
        return userService.increaseBalance(id, req.getAmount());
    }

    @Data
    public static class ChargeRequest {
        private BigDecimal amount;
    }
}

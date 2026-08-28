package com.example.points.controller;

import com.example.points.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping
    public String register(@RequestParam String username,
                           @RequestParam String mobile) {
        Long userId = registerService.register(username, mobile);
        return "register success, userId=" + userId;
    }
}

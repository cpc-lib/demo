package com.example.points.controller;

import com.example.points.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Long register(@RequestBody RegisterReq req) throws JsonProcessingException {
        return userService.registerUser(req.getUsername(), req.getMobile());
    }

    @Data
    public static class RegisterReq {
        @NotBlank
        private String username;

        private String mobile;
    }
}

package com.example.points.controller;

import com.example.points.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Long register(@RequestParam String username,
                         @RequestParam String mobile) throws JsonProcessingException {
        return userService.registerUser(username, mobile);
    }
}

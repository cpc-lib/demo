package com.example.user.controller;

import com.example.user.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public SignupResp signup(@RequestBody SignupReq req) {
        Long userId = userService.registerUser(req.getUsername(), req.getPhone());
        SignupResp resp = new SignupResp();
        resp.setUserId(userId);
        resp.setMsg("注册成功，赠送积分会稍后到账");
        return resp;
    }

    @Data
    public static class SignupReq {
        private String username;
        private String phone;
    }

    @Data
    public static class SignupResp {
        private Long userId;
        private String msg;
    }
}

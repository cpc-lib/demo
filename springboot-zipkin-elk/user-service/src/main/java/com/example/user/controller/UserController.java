package com.example.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable("id") Long id) {
        log.info("开始处理用户查询，userId={}", id);

        Map<String, Object> user = new HashMap<>();
        user.put("userId", id);
        user.put("username", "user-" + id);
        user.put("phone", "1380013800" + id % 10);

        log.info("用户查询完成，userId={}", id);
        return user;
    }
}

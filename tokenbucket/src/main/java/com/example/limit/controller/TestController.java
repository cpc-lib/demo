package com.example.limit.controller;

import com.example.limit.service.TestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
public class TestController {
    @Resource
    TestService s;

    @GetMapping("/call")
    public Map<String,Object> call() {
        return s.call();
    }
}

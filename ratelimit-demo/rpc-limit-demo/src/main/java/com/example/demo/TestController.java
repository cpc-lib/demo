package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    private final ExternalServiceCaller caller;

    public TestController(ExternalServiceCaller caller){
        this.caller = caller;
    }

    @GetMapping("/test")
    public Map<String,Object> test(){
        return caller.call();
    }
}

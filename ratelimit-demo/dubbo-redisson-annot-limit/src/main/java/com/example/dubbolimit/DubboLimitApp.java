package com.example.dubbolimit;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableDubbo
public class DubboLimitApp {
    public static void main(String[] args) {
        SpringApplication.run(DubboLimitApp.class, args);
    }
}

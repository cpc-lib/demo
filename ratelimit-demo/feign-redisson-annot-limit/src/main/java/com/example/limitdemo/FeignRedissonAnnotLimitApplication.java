package com.example.limitdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableFeignClients
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class FeignRedissonAnnotLimitApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeignRedissonAnnotLimitApplication.class, args);
    }
}

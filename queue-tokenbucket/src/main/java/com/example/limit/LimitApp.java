package com.example.limit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 启动类
 *
 * 开启 @EnableAspectJAutoProxy 让 AOP 生效，
 * 从而可以拦截我们自定义的 @QueueTokenLimit 注解。
 */
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class LimitApp {

    public static void main(String[] args) {
        SpringApplication.run(LimitApp.class, args);
    }
}

package com.example.pointsdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.pointsdemo.mapper")
public class RegisterPointsDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegisterPointsDemoApplication.class, args);
    }
}
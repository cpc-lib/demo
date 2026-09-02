package com.example.sha256.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.example.sha256")
public class Sha256ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(Sha256ApiApplication.class, args);
    }
}

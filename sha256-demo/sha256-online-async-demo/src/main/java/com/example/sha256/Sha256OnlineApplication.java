package com.example.sha256;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Sha256OnlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(Sha256OnlineApplication.class, args);
    }
}

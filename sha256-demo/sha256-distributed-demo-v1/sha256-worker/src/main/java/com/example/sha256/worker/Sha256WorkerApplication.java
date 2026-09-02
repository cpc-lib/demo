package com.example.sha256.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.sha256")
public class Sha256WorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(Sha256WorkerApplication.class, args);
    }
}

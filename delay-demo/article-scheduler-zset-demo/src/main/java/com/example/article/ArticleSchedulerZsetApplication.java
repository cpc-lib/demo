package com.example.article;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArticleSchedulerZsetApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleSchedulerZsetApplication.class, args);
    }
}

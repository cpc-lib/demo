package com.example.article;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.article.mapper")
public class ArticleSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleSchedulerApplication.class, args);
    }
}

package com.example.articlescheduler;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.articlescheduler.mapper")
public class ArticleTimerDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleTimerDemoApplication.class, args);
    }
}

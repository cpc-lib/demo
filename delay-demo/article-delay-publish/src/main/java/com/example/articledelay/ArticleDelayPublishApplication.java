package com.example.articledelay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class ArticleDelayPublishApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleDelayPublishApplication.class, args);
    }
}

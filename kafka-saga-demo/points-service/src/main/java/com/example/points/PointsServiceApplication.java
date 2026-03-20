package com.example.points;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.points.mapper")
public class PointsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PointsServiceApplication.class, args);
    }


    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }
}

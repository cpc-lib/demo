package com.example.points;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.points.mapper")
public class RabbitmqPointsDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitmqPointsDemoApplication.class, args);
    }
}

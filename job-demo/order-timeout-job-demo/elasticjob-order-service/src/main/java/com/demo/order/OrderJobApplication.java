package com.demo.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.demo.order.mapper")
public class OrderJobApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderJobApplication.class, args);
    }
}

package com.example.idem.caller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(
        scanBasePackages = "com.example.idem",
        exclude = DataSourceAutoConfiguration.class)
public class CallerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CallerServiceApplication.class, args);
    }
}

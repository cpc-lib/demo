package com.example.versionedcachemp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.versionedcachemp.mapper")
public class VersionedCacheRedisMqMysqlMpDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VersionedCacheRedisMqMysqlMpDemoApplication.class, args);
    }
}

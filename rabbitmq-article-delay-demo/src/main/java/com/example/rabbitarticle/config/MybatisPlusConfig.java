package com.example.rabbitarticle.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.rabbitarticle.mapper")
public class MybatisPlusConfig {
}

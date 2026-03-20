package com.example.article.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.article.mapper")
public class MybatisPlusConfig {
}

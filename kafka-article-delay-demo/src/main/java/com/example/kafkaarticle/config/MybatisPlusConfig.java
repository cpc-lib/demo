package com.example.kafkaarticle.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.kafkaarticle.mapper")
public class MybatisPlusConfig {
}

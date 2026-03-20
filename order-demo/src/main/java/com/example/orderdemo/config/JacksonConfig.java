package com.example.orderdemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    ObjectMapper om = builder.createXmlMapper(false).build();
    om.registerModule(new JavaTimeModule());
    // 禁用时间戳输出，使用可读的字符串
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return om;
  }
}

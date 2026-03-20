package com.example.orderdemo.config;

import com.example.orderdemo.no.OrderNoGenerator;
import com.example.orderdemo.no.impl.SnowflakeOrderNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OrderNoConfig {

  private final SnowflakeOrderNoGenerator snowflake;

  @Bean
  public OrderNoGenerator orderNoGenerator() {
    return snowflake;
  }
}

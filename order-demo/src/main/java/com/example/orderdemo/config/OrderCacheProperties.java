package com.example.orderdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "order.cache")
public class OrderCacheProperties {
  private long detailTtlSeconds = 300;
  private long versionTtlSeconds = 86400;
  private long lockTtlMs = 3000;
}

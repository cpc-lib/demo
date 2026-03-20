package com.example.orderdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "order.no")
public class OrderNoProperties {

  /** SNOWFLAKE | SEGMENT | LEAF */
  private String strategy = "SNOWFLAKE";

  /** 订单号前缀，例如 OD */
  private String prefix = "OD";

  private Snowflake snowflake = new Snowflake();

  @Data
  public static class Snowflake {
    /** 0~31 */
    private long workerId = 1;
    /** 0~31 */
    private long datacenterId = 1;
  }
}

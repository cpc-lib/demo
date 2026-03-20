package com.example.orderdemo.no.impl;

import com.example.orderdemo.config.OrderNoProperties;
import com.example.orderdemo.no.OrderNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnowflakeOrderNoGenerator implements OrderNoGenerator {

  private final OrderNoProperties props;

  private volatile SnowflakeIdWorker worker;

  @Override
  public String nextOrderNo() {
    // 延迟初始化：拿到配置里的 workerId/datacenterId
    SnowflakeIdWorker w = worker;
    if (w == null) {
      synchronized (this) {
        w = worker;
        if (w == null) {
          long workerId = props.getSnowflake().getWorkerId();
          long datacenterId = props.getSnowflake().getDatacenterId();
          worker = w = new SnowflakeIdWorker(workerId, datacenterId);
        }
      }
    }
    long id = w.nextId();
    return props.getPrefix() + id;
  }
}

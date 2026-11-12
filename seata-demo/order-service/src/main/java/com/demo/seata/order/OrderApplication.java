package com.demo.seata.order;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单服务启动类 (TM 事务协调发起端)
 */
@EnableDubbo
@SpringBootApplication(scanBasePackages = {"com.demo.seata.order", "com.demo.seata.common"})
@MapperScan({"com.demo.seata.order.mapper", "com.demo.seata.common.mapper"})
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}

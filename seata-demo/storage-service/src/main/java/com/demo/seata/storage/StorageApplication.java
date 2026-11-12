package com.demo.seata.storage;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 库存服务启动类
 */
@EnableDubbo
@SpringBootApplication(scanBasePackages = {"com.demo.seata.storage", "com.demo.seata.common"})
@MapperScan({"com.demo.seata.storage.mapper", "com.demo.seata.common.mapper"})
public class StorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageApplication.class, args);
    }
}

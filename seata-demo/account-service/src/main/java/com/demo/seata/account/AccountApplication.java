package com.demo.seata.account;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 账户服务启动类
 */
@EnableDubbo
@SpringBootApplication(scanBasePackages = {"com.demo.seata.account", "com.demo.seata.common"})
@MapperScan({"com.demo.seata.account.mapper", "com.demo.seata.common.mapper"})
public class AccountApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountApplication.class, args);
    }
}

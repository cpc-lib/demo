package cc.ivera;

import org.dromara.easyes.starter.register.EsMapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 * <p>
 * Copyright © 2021 xpc1024 All Rights Reserved
 **/
@SpringBootApplication
@EsMapperScan("cc.ivera.eeuse.mapper")
public class EeUseApplication {

    public static void main(String[] args) {
        SpringApplication.run(EeUseApplication.class, args);
    }

}

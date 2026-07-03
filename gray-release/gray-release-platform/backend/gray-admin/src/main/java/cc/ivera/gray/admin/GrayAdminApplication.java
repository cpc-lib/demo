package cc.ivera.gray.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cc.ivera.gray.admin.mapper")
@EnableScheduling
public class GrayAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrayAdminApplication.class, args);
    }
}

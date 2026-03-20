package cc.ivera;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cpc.mapper")
public class ActApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActApplication.class, args);
    }
}

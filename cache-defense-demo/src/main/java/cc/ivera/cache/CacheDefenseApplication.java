
package cc.ivera.cache;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cc.ivera.cache.mapper")
public class CacheDefenseApplication {
    public static void main(String[] args) {
        SpringApplication.run(CacheDefenseApplication.class, args);
    }
}

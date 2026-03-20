package cc.ivera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @version v1.0
 * @description
 * @since 2020/2/5 12:21
 */
@SpringBootApplication(scanBasePackages = "cc.ivera")
public class TransactionalMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionalMessageApplication.class, args);
    }
}

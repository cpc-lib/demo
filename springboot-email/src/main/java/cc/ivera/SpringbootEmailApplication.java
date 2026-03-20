package cc.ivera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringbootEmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootEmailApplication.class, args);
    }

}

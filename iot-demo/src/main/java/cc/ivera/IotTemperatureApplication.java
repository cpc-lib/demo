package cc.ivera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IotTemperatureApplication {
    public static void main(String[] args) {
        SpringApplication.run(IotTemperatureApplication.class, args);
    }
}

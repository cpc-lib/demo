package cc.ivera;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class PaymentDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentDemoApplication.class, args);
    }

}

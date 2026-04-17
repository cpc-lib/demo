package cc.ivera.ordermachine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cc.ivera.ordermachine.mapper")
public class OrderMachineApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderMachineApplication.class, args);
    }
}

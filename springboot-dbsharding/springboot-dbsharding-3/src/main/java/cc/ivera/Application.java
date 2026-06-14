package cc.ivera;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//jdk1.8_352
@SpringBootApplication
@MapperScan("cc.ivera.mapper")
public class Application {
	
	public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}

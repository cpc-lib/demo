package cc.ivera.seata.storage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * storage 启动类
 * 
 * @author sly
 * @time 2019年6月12日
 */
@EnableEurekaClient
@SpringBootApplication
@MapperScan("cc.ivera.seata.storage.mapper")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

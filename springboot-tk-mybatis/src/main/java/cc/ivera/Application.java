package cc.ivera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import tk.mybatis.spring.annotation.MapperScan;

/**
 * 引导类。 SpringBoot项目的入口
 * 默认只能扫此文件所在的包。
 * 即如果在top.arhi下创建app文件，并放入此文件
 * 需要添加一个注解扫描
 * @ComponentScan("cc.ivera")
 */
@SpringBootApplication
@MapperScan("cc.ivera.mapper")
@org.mybatis.spring.annotation.MapperScan("cc.ivera.mapper")
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
    }

}

package cc.ivera;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
@MapperScan("cc.ivera.mapper")
public class SpringbootEasyexcelApplication extends SpringBootServletInitializer {

    /**
     * 外部tomcat访问的程序入口
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(SpringbootEasyexcelApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringbootEasyexcelApplication.class, args);
    }

}

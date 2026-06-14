package cc.ivera;

import cc.ivera.service.UserProfileService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.jsf.el.SpringBeanFacesELResolver;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {

        ConfigurableApplicationContext context =
                SpringApplication.run(DemoApplication.class, args);

        // 获取 Bean
        UserProfileService service = context.getBean(UserProfileService.class);

        // 调用方法测试
        Long id = service.addDemoUser();
        System.out.println("新增用户ID：" + id);

    }
}

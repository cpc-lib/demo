package cc.ivera.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import cc.ivera.properties.IPCntProperties;
import cc.ivera.service.IPCntService;
import cc.ivera.service.impl.IPCntServiceImpl;

//@ComponentScan注解默认扫描@SpringBootApplication所在路径下所有注解加入到Spring容器中
@EnableScheduling
//@EnableConfigurationProperties(IPCntProperties.class) 不能使用该注解，读取到的beanName不是iPCntProperties(解决SpringEL表达式的bug)
@Import({IPCntProperties.class,SpringMvcConfig.class}) //添加SpringMvgConfig.class,自己进行加载。(spring.factories在读取到此配置类，将bean都注入到容器中)
public class IPCntAutoConfiguration {
    @Bean
    private IPCntService ipCntService(){
        return (IPCntService) new IPCntServiceImpl();
    }
}

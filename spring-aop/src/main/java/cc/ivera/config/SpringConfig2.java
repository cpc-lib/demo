package cc.ivera.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@Configuration
@ComponentScan("top.arhi")
/**
 * 开启aop编程支持
 */
@EnableAspectJAutoProxy
public class SpringConfig2 {
}

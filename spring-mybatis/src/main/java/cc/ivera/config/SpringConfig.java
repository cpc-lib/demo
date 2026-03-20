package cc.ivera.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MyBatisConfig.class,JdbcConfig.class})
@ComponentScan("cc.ivera")
public class SpringConfig {
}

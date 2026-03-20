package cc.ivera.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@ComponentScan({"cc.ivera.controller","cc.ivera.config"})
/**
 * 允许类型的转换
 */
@EnableWebMvc
public class SpringmvcConfig {
}

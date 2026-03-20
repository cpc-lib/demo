package cc.ivera.config;

import cc.ivera.interceptor.IPCntInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = true) //加载同一个bean
public class SpringMvcConfig implements WebMvcConfigurer {

//    @Autowired
//    private IPCntInterceptor ipCntInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipCntInterceptor()).addPathPatterns("/**");
    }

    @Bean
    public IPCntInterceptor ipCntInterceptor(){
        return new IPCntInterceptor();
    }

}

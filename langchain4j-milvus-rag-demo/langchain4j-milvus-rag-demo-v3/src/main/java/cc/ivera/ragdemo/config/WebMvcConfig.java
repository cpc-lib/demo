package cc.ivera.ragdemo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类，注册日志拦截器和限流拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册日志拦截器，应用于所有请求（先执行）
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                // 排除静态资源和公共路径
                .excludePathPatterns(
                        "/error",
                        "/favicon.ico",
                        "/assets/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/**"
                );

        // 注册限流拦截器，应用于所有请求（后执行）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                // 排除静态资源、API文档和健康检查
                .excludePathPatterns(
                        "/error",
                        "/favicon.ico",
                        "/assets/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/**"
                );
    }
}
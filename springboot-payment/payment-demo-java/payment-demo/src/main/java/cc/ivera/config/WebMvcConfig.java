package cc.ivera.config;

import cc.ivera.security.AdminInterceptor;
import cc.ivera.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final AdminInterceptor adminInterceptor;

    private final AuthProperties authProperties;

    public WebMvcConfig(
            AuthInterceptor authInterceptor,
            AdminInterceptor adminInterceptor,
            AuthProperties authProperties
    ) {
        this.authInterceptor = authInterceptor;
        this.adminInterceptor = adminInterceptor;
        this.authProperties = authProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/product/**",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/wx-pay/native/notify",
                        "/api/wx-pay/jsapi/notify/v1",
                        "/api/wx-pay/refunds/notify",
                        "/api/wx-pay-v2/native/notify",
                        "/api/ali-pay/trade/notify"
                );

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns(
                        "/api/payment-config/**",
                        "/api/payment-channel/**",
                        "/api/payment-app/**",
                        "/api/bill/**",
                        "/api/reconciliation/**",
                        "/api/order-info/list",
                        "/api/refund-info/list",
                        "/api/refund-info/approve/**",
                        "/api/refund-info/reject/**",
                        "/api/refund-info/query/**",
                        "/api/refund-info/reconcile/**",
                        "/api/wx-pay/check-order-status/**",
                        "/api/ali-pay/check-order-status/**",
                        "/api/wx-pay/querybill/**",
                        "/api/wx-pay/downloadbill/**",
                        "/api/ali-pay/bill/**"
                )
                .excludePathPatterns("/api/payment-app/list");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(authProperties.getAllowedOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

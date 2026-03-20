package cc.ivera.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import cc.ivera.consumer.config.MyRule;

@EnableDiscoveryClient // 激活DiscoveryClient
@EnableEurekaClient
@SpringBootApplication
@EnableFeignClients //开启Feign的功能
@RefreshScope
//配置负载均衡方式
@RibbonClient(name = "GATEWAY-PROVIDER", configuration = MyRule.class)
public class ConsumerApp {


    public static void main(String[] args) {
        SpringApplication.run(ConsumerApp.class, args);
    }


}

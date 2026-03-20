package cc.ivera.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * TODO 需要基于springboot-websocket-smiple进行改造
 */
@Configuration
public class StompConfig {
    //这个bean的注册,用于扫描带有@ServerEndpoint的注解成为websocket
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

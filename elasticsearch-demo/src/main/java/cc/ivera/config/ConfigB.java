package cc.ivera.config;

import lombok.*;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

//@Component
@Configuration
@ConfigurationProperties(prefix="elasticsearch")
//使用lombook，使用@Data注解，引入Setter注解
//使用了AllArgsConstructor必须使用NoArgsConstructor
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigB {
    private String hostname;
    private Integer port;
    private String schema;

    @Bean
    @Qualifier
    public RestHighLevelClient clientB(){
        return new RestHighLevelClient(RestClient.builder(
                new HttpHost(hostname,port,schema)));
    }


}

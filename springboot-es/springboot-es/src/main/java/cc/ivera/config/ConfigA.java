package cc.ivera.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigA {

    @Value("${elasticsearch.hostname}")
    private String hostname;
    @Value("${elasticsearch.port}")
    private Integer port;
    @Value("${elasticsearch.schema}")
    private String schema;

    @Bean
    public RestHighLevelClient elasticClient(){

        System.out.println("创建bean交给springboot处理");
        return new RestHighLevelClient(RestClient.builder(
                new HttpHost(hostname,port,schema)
        ));
    }
}

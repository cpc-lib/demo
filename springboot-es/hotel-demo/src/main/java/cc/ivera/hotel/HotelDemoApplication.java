package cc.ivera.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@MapperScan("cc.ivera.hotel.mapper")
@SpringBootApplication
public class HotelDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelDemoApplication.class, args);
    }
    @Bean
    public RestHighLevelClient client(
            @Value("${ELASTICSEARCH_HOST:127.0.0.1}") String host,
            @Value("${ELASTICSEARCH_PORT:9200}") int port){
        return  new RestHighLevelClient(RestClient.builder(
                new HttpHost(host, port)
        ));
    }
}

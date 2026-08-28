package cc.ivera.userdemo.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfig {

  @Bean(destroyMethod = "close")
  public RestHighLevelClient restHighLevelClient(@Value("${app.es.hosts}") String hosts) {
    // 支持逗号分隔
    String[] arr = hosts.split(",");
    HttpHost[] httpHosts = new HttpHost[arr.length];
    for (int i = 0; i < arr.length; i++) {
      httpHosts[i] = HttpHost.create(arr[i].trim());
    }
    return new RestHighLevelClient(RestClient.builder(httpHosts));
  }
}

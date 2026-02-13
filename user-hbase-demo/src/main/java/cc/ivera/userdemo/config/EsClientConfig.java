package cc.ivera.userdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@Slf4j
@org.springframework.context.annotation.Configuration
public class EsClientConfig {

  @Bean(destroyMethod = "close")
  public RestHighLevelClient esClient(AppProperties props) {
    String hosts = props.getEs().getHosts();
    HttpHost[] httpHosts = Arrays.stream(hosts.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(hp -> {
          String[] arr = hp.split(":");
          return new HttpHost(arr[0], Integer.parseInt(arr[1]), "http");
        })
        .toArray(HttpHost[]::new);

    log.info("ES client hosts={}", hosts);
    return new RestHighLevelClient(RestClient.builder(httpHosts));
  }
}

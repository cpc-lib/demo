package com.example.orderdemo.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class ElasticsearchConfig {

  @Bean(destroyMethod = "close")
  public RestHighLevelClient restHighLevelClient(@Value("${app.es.hosts}") String hosts) {
    HttpHost[] httpHosts = Arrays.stream(hosts.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(h -> {
          String[] parts = h.split(":");
          String host = parts[0];
          int port = Integer.parseInt(parts[1]);
          return new HttpHost(host, port, "http");
        })
        .toArray(HttpHost[]::new);

    return new RestHighLevelClient(RestClient.builder(httpHosts));
  }
}

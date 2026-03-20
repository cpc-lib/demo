package cc.ivera.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
public class ConfigC {
    private String hostname;
    private Integer port;
    private String schema;

    public ConfigC() {
    }

    public ConfigC(String hostname, Integer port, String schema) {
        this.hostname = hostname;
        this.port = port;
        this.schema = schema;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Bean
    public RestHighLevelClient clientC() {
        return new RestHighLevelClient(RestClient.builder(
                new HttpHost(hostname, port, schema)));
    }
}

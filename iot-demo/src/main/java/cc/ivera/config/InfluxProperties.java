package cc.ivera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "influxdb")
public record InfluxProperties(String url, String token, String org, String bucket) {
}

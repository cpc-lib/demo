package cc.ivera.userdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private HBase hbase = new HBase();
  private Es es = new Es();
  private Kafka kafka = new Kafka();
  private Retry retry = new Retry();

  @Data
  public static class HBase {
    private String zookeeperQuorum;
    private int zookeeperPort = 2181;
    private String znodeParent = "/hbase";
    private boolean init = true;
  }

  @Data
  public static class Es {
    private String hosts; // host:port,host:port
    private String index = "user_profile";
    private boolean init = true;
  }

  @Data
  public static class Kafka {
    private String topicUserUpserted = "user-upserted";
  }

  @Data
  public static class Retry {
    private int maxRetries = 3;
    private long scanIntervalMs = 5000;
  }
}

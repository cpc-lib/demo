package cc.ivera.studentdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.hbase")
public class HBaseProps {
  /**
   * ZooKeeper quorum, e.g. 127.0.0.1
   */
  private String quorum = "127.0.0.1";
  /**
   * ZooKeeper port, e.g. 2181 (docker-compose in this demo maps host 2182 -> container 2181)
   */
  private String port = "2182";
  /**
   * Optional znode parent, e.g. /hbase
   */
  private String znodeParent;
}

package cc.ivera.studentdemo.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(HBaseProps.class)
public class HBaseConfig {

  @Bean(destroyMethod = "close")
  public Connection hbaseConnection(HBaseProps props) throws IOException {
    Configuration conf = HBaseConfiguration.create();
    conf.set("hbase.zookeeper.quorum", props.getQuorum());
    conf.set("hbase.zookeeper.property.clientPort", props.getPort());
    if (props.getZnodeParent() != null && !props.getZnodeParent().isBlank()) {
      conf.set("zookeeper.znode.parent", props.getZnodeParent());
    }
    conf.setInt("hbase.rpc.timeout", 5000);
    conf.setInt("hbase.client.operation.timeout", 5000);
    conf.setInt("hbase.client.scanner.timeout.period", 10000);
    return ConnectionFactory.createConnection(conf);
  }
}

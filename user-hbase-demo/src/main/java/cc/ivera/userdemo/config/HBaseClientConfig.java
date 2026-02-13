package cc.ivera.userdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@Slf4j
@org.springframework.context.annotation.Configuration
public class HBaseClientConfig {

  @Bean(destroyMethod = "close")
  public Connection hbaseConnection(AppProperties props) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    conf.set("hbase.zookeeper.quorum", props.getHbase().getZookeeperQuorum());
    conf.setInt("hbase.zookeeper.property.clientPort", props.getHbase().getZookeeperPort());
    conf.set("zookeeper.znode.parent", props.getHbase().getZnodeParent());
    // 客户端超时可按需调大
    conf.setInt("hbase.rpc.timeout", 30000);
    conf.setInt("hbase.client.operation.timeout", 30000);
    conf.setInt("hbase.client.scanner.timeout.period", 10000);

    Connection conn = ConnectionFactory.createConnection(conf);
    log.info("HBase connection created. zk={}:{}, znodeParent={}",
        props.getHbase().getZookeeperQuorum(), props.getHbase().getZookeeperPort(), props.getHbase().getZnodeParent());
    return conn;
  }
}

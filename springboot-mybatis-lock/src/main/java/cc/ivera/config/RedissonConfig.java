package cc.ivera.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import cc.ivera.properties.RedisProperties;

@Configuration
@EnableConfigurationProperties({RedisProperties.class})
public class RedissonConfig {

    @Autowired
    private RedisProperties redisProperties;

    @Bean
    public RedissonClient getRedissonClient() {
        //获取当前redis节点信息
        String[] nodes = redisProperties.getNodes().split(",");
        //创建配置信息：1、单机redis配置 2、集群redis配置
        Config config = new Config();
        if (nodes.length==1){
            //1、单机redis配置
            config.useSingleServer().setAddress(nodes[0])
                    .setConnectTimeout(redisProperties.getConnectTimeout())
                    .setConnectionMinimumIdleSize(redisProperties.getConnectionMinimumidleSize())
                    .setConnectionPoolSize(redisProperties.getConnectPoolSize())
                    .setTimeout(redisProperties.getTimeout());
        }else if(nodes.length>1) {
            //2、集群redis配置
            config.useClusterServers().addNodeAddress(nodes)
                    .setConnectTimeout(redisProperties.getConnectTimeout())
                    .setMasterConnectionMinimumIdleSize(redisProperties.getConnectionMinimumidleSize())
                    .setMasterConnectionPoolSize(redisProperties.getConnectPoolSize())
                    .setTimeout(redisProperties.getTimeout());
        }else {
            return null;
        }
        //创建redission的客户端，交于spring管理
        RedissonClient client = Redisson.create(config);
        return client;
    }
}

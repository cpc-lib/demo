package cc.ivera.config;

import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;


@Configuration
public class RedissonConfig {

    Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    @Resource
    private RedisProperties redisProperties;

    private int timeout = 3000;
    private int connectionPoolSize = 64;
    private int connectionMinimumIdleSize = 10;
    private int pingConnectionInterval = 60000;
    private static String ADDRESS_PREFIX = "redis://";

    /**
     * 单机模式
     */
    @Bean
    public RedissonClient redissonClient() {
        // 哨兵模式
        RedisProperties.Sentinel sentinel = redisProperties.getSentinel();
        if (Objects.nonNull(sentinel)) {
            log.info("redis is sentinel mode");
            return redissonSentinel();
        }
        // 集群模式
        RedisProperties.Cluster cluster = redisProperties.getCluster();
        if (Objects.nonNull(cluster)) {
            log.info("redis is cluster mode");
            return redissonCluster();
        }
        // 单机模式
        String host = redisProperties.getHost();
        if (StringUtils.isNotBlank(host)) {
            log.info("redis is single mode");
            return redissonSingle();
        }

        log.error("redisson config can not support this redis mode");
        return null;
    }

    /**
     * 单机模式
     */
    private RedissonClient redissonSingle() {
        String host = redisProperties.getHost();
        String password = redisProperties.getPassword();
        int port = redisProperties.getPort();
        // 设置超时时间
        if (redisProperties.getTimeout().getSeconds() * 1000 > 0L) {
            Long value = new Long(redisProperties.getTimeout().getSeconds() * 1000);
            timeout = new Integer(value.toString());
        }
        // 声明一个配置类
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer().setAddress(ADDRESS_PREFIX + host + ":" + port).setTimeout(timeout).setPingConnectionInterval(pingConnectionInterval).setConnectionPoolSize(this.connectionPoolSize).setConnectionMinimumIdleSize(this.connectionMinimumIdleSize);
        // 判断密码
        if (!StringUtils.isEmpty(password)) {
            serverConfig.setPassword(password);
        }
        return Redisson.create(config);
    }


    /**
     * 哨兵模式
     */
    private RedissonClient redissonSentinel() {
        String masterName = redisProperties.getSentinel().getMaster();
        List<String> nodes = redisProperties.getSentinel().getNodes();
        String password = redisProperties.getPassword();
        // 设置超时时间
        if (redisProperties.getTimeout().getSeconds() * 1000 > 0L) {
            Long value = new Long(redisProperties.getTimeout().getSeconds() * 1000);
            timeout = new Integer(value.toString());
        }
        // 声明一个配置类
        Config config = new Config();
        SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
        // 扫描间隔
        sentinelServersConfig.setScanInterval(2000);
        // 判断密码
        if (StringUtils.isNotEmpty(password)) {
            sentinelServersConfig.setPassword(password);
        }
        sentinelServersConfig.setMasterName(masterName);
        String[] nodeArr = {};
        for (int i = 0; i < nodes.size(); i++) {
            nodeArr[i] = ADDRESS_PREFIX + nodeArr[i];
        }
        // 添加redis节点
        sentinelServersConfig.addSentinelAddress(nodeArr);
        return Redisson.create(config);
    }

    /**
     * 集群模式
     */
    private RedissonClient redissonCluster() {
        List<String> nodes = redisProperties.getCluster().getNodes();
        String password = redisProperties.getPassword();
        // 设置超时时间
        if (redisProperties.getTimeout().getSeconds() * 1000 > 0L) {
            Long value = new Long(redisProperties.getTimeout().getSeconds() * 1000);
            timeout = new Integer(value.toString());
        }
        // 声明一个配置类
        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers();
        // 扫描间隔
        clusterServersConfig.setScanInterval(2000);
        // 判断密码
        if (StringUtils.isNotEmpty(password)) {
            clusterServersConfig.setPassword(password);
        }
        // 添加redis节点
        for (String node : nodes) {
            clusterServersConfig.addNodeAddress(ADDRESS_PREFIX + node);
        }
        return Redisson.create(config);
    }
}
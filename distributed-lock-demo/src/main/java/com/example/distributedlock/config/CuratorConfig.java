package com.example.distributedlock.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CuratorConfig {

    @Bean(destroyMethod = "close")
    CuratorFramework curatorFramework(
            @Value("${app.lock.zookeeper.connect-string}") String connectString,
            @Value("${app.lock.zookeeper.session-timeout-ms:30000}") int sessionTimeoutMs,
            @Value("${app.lock.zookeeper.connection-timeout-ms:10000}") int connectionTimeoutMs) {

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(sessionTimeoutMs)
                .connectionTimeoutMs(connectionTimeoutMs)
                .retryPolicy(new ExponentialBackoffRetry(500, 3))
                .build();
        client.start();
        return client;
    }
}

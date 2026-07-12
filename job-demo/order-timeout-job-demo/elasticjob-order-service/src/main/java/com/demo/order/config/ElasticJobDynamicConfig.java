package com.demo.order.config;

import com.demo.order.job.ElasticCloseTimeoutOrderJob;
import com.demo.order.mapper.JobCronConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.apache.shardingsphere.elasticjob.lite.api.bootstrap.impl.ScheduleJobBootstrap;
import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
import org.apache.shardingsphere.elasticjob.reg.zookeeper.ZookeeperConfiguration;
import org.apache.shardingsphere.elasticjob.reg.zookeeper.ZookeeperRegistryCenter;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ElasticJobDynamicConfig {
    private final JobCronConfigMapper cronConfigMapper;
    private final ElasticCloseTimeoutOrderJob job;

    @Value("${elasticjob.reg-center.server-lists}")
    private String serverLists;
    @Value("${elasticjob.reg-center.namespace}")
    private String namespace;

    @Bean(initMethod = "init")
    public CoordinatorRegistryCenter registryCenter() {
        ZookeeperConfiguration zkConfig = new ZookeeperConfiguration(serverLists, namespace);
        zkConfig.setConnectionTimeoutMilliseconds(60000);
        zkConfig.setSessionTimeoutMilliseconds(60000);
        zkConfig.setMaxRetries(3);
        zkConfig.setMaxSleepTimeMilliseconds(20000);
        return new ZookeeperRegistryCenter(zkConfig);
    }

    @Bean
    public CommandLineRunner elasticJobRunner(CoordinatorRegistryCenter registryCenter) {
        return args -> {
            var config = cronConfigMapper.selectByJobType("ELASTIC");
            String cron = config == null ? "0 */5 * * * ?" : config.getCronExpr();
            JobConfiguration jobConfig = JobConfiguration.newBuilder("elasticCloseTimeoutUnpaidOrder", 1)
                    .cron(cron)
                    .overwrite(true)
                    .build();
            new ScheduleJobBootstrap(registryCenter, job, jobConfig).schedule();
        };
    }
}

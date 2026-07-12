package com.example.orderclose.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XxlJobConfig {

    private static final Logger log = LoggerFactory.getLogger(XxlJobConfig.class);

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.admin.timeout:3}")
    private int adminTimeout;

    @Value("${xxl.job.executor.enabled:true}")
    private boolean enabled;

    @Value("${xxl.job.executor.appname}")
    private String appName;

    @Value("${xxl.job.executor.address:}")
    private String address;

    @Value("${xxl.job.executor.ip:}")
    private String ip;

    @Value("${xxl.job.executor.port:9999}")
    private int port;

    @Value("${xxl.job.executor.accessToken}")
    private String accessToken;

    @Value("${xxl.job.executor.logpath:./logs/xxl-job}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:30}")
    private int logRetentionDays;

    @Value("${xxl.job.executor.excludedpackage:org.springframework,spring}")
    private String excludedPackage;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("Initializing XXL-JOB executor, appName={}, adminAddresses={}, address={}, port={}",
                appName, adminAddresses, address, port);

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setTimeout(adminTimeout);
        executor.setEnabled(enabled);
        executor.setAppname(appName);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        executor.setExcludedPackage(excludedPackage);
        return executor;
    }
}

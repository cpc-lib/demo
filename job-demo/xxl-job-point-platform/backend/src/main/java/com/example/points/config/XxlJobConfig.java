package com.example.points.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XxlJobConfig {
    @Value("${xxl.job.admin.addresses}")
    String admin;
    @Value("${xxl.job.admin.timeout:3}")
    int timeout;
    @Value("${xxl.job.executor.enabled:true}")
    Boolean enabled;
    @Value("${xxl.job.executor.appname}")
    String appname;
    @Value("${xxl.job.executor.accessToken}")
    String token;
    @Value("${xxl.job.executor.ip:}")
    String ip;
    @Value("${xxl.job.executor.port:9999}")
    int port;
    @Value("${xxl.job.executor.address:}")
    String address;
    @Value("${xxl.job.executor.logpath:./logs/xxl-job}")
    String logpath;
    @Value("${xxl.job.executor.logretentiondays:30}")
    int retention;

    @Bean
    XxlJobSpringExecutor xxlJobExecutor() {
        var e = new XxlJobSpringExecutor();
        e.setAdminAddresses(admin);
        e.setTimeout(timeout);
        e.setEnabled(enabled);
        e.setAppname(appname);
        e.setAccessToken(token);
        e.setIp(ip);
        e.setPort(port);
        e.setAddress(address);
        e.setLogPath(logpath);
        e.setLogRetentionDays(retention);
        return e;
    }
}

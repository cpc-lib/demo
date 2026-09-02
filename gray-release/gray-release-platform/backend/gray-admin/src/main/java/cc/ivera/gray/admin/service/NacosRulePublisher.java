package cc.ivera.gray.admin.service;

import cc.ivera.gray.admin.entity.GrayRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import java.util.List;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NacosRulePublisher {
    private static final String GROUP = "GRAY_RELEASE";

    private final ObjectMapper objectMapper;
    private final String serverAddr;

    public NacosRulePublisher(ObjectMapper objectMapper,
                              @Value("${spring.cloud.nacos.config.server-addr:192.168.1.200:8848}") String serverAddr) {
        this.objectMapper = objectMapper;
        this.serverAddr = serverAddr;
    }

    public boolean publish(String serviceId, List<GrayRule> rules) {
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", serverAddr);
            ConfigService configService = NacosFactory.createConfigService(properties);
            String dataId = serviceId + "-gray-rules.json";
            return configService.publishConfig(dataId, GROUP, objectMapper.writeValueAsString(rules));
        } catch (Exception ex) {
            throw new IllegalStateException("发布 Nacos 灰度规则失败：" + ex.getMessage(), ex);
        }
    }
}


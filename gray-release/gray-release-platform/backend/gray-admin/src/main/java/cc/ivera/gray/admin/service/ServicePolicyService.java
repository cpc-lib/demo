package cc.ivera.gray.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.gray.admin.entity.ServicePolicy;
import cc.ivera.gray.admin.mapper.ServicePolicyMapper;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicePolicyService {
    private final ServicePolicyMapper servicePolicyMapper;
    private final AuditService auditService;

    public ServicePolicyService(ServicePolicyMapper servicePolicyMapper, AuditService auditService) {
        this.servicePolicyMapper = servicePolicyMapper;
        this.auditService = auditService;
    }

    public ServicePolicy getOrCreate(String serviceId) {
        ServicePolicy policy = servicePolicyMapper.selectOne(new LambdaQueryWrapper<ServicePolicy>()
                .eq(ServicePolicy::getServiceId, serviceId)
                .last("limit 1"));
        if (policy != null) {
            return policy;
        }
        ServicePolicy created = new ServicePolicy();
        created.setServiceId(serviceId);
        created.setDefaultVersion("v1");
        created.setBlueVersion("v1");
        created.setGreenVersion("v2");
        created.setActiveColor("blue");
        created.setAbEnabled(false);
        created.setAbVersionA("v1");
        created.setAbVersionB("v2");
        created.setAbPercentB(50);
        servicePolicyMapper.insert(created);
        return created;
    }

    @Transactional
    public ServicePolicy blueGreenSwitch(String serviceId, String activeColor, String operator) {
        ServicePolicy policy = getOrCreate(serviceId);
        String color = activeColor.toLowerCase(Locale.ROOT);
        if (!"blue".equals(color) && !"green".equals(color)) {
            throw new IllegalArgumentException("activeColor 只能是 blue 或 green");
        }
        String before = policy.getDefaultVersion();
        policy.setActiveColor(color);
        policy.setDefaultVersion("green".equals(color) ? policy.getGreenVersion() : policy.getBlueVersion());
        servicePolicyMapper.updateById(policy);
        auditService.record(operator, "BLUE_GREEN_SWITCH", "SERVICE_POLICY", serviceId, before, policy.getDefaultVersion());
        return servicePolicyMapper.selectById(policy.getId());
    }

    @Transactional
    public ServicePolicy configureAb(String serviceId, boolean enabled, int percentB, String operator) {
        ServicePolicy policy = getOrCreate(serviceId);
        policy.setAbEnabled(enabled);
        policy.setAbPercentB(Math.max(0, Math.min(100, percentB)));
        servicePolicyMapper.updateById(policy);
        auditService.record(operator, "CONFIGURE_AB", "SERVICE_POLICY", serviceId, null,
                "enabled=" + enabled + ", percentB=" + policy.getAbPercentB());
        return servicePolicyMapper.selectById(policy.getId());
    }
}


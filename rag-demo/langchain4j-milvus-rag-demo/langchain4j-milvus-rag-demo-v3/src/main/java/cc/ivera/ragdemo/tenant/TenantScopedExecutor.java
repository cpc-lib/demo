package cc.ivera.ragdemo.tenant;


import cc.ivera.ragdemo.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TenantScopedExecutor {

    private final RagProperties properties;

    public void runAsSystemTenant(Long tenantId, String reason, Runnable runnable) {
        TenantContextHolder.runWith(TenantContextHolder.systemContext(effectiveTenantId(tenantId), reason), runnable);
    }

    public <T> T callAsSystemTenant(Long tenantId, String reason, Supplier<T> supplier) {
        return TenantContextHolder.callWith(TenantContextHolder.systemContext(effectiveTenantId(tenantId), reason), supplier);
    }

    private Long effectiveTenantId(Long tenantId) {
        if (tenantId != null) {
            return tenantId;
        }
        return properties.getIngestion().getDefaultTenantId();
    }
}

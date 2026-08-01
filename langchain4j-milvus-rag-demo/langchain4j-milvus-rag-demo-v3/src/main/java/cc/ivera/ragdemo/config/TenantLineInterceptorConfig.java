package cc.ivera.ragdemo.config;


import cc.ivera.ragdemo.exception.MissingTenantContextException;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TenantLineInterceptorConfig {

    private final RagProperties properties;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (properties.getTenant().isMybatisTenantInterceptorEnabled()) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override
                public Expression getTenantId() {
                    Long tenantId = TenantContextHolder.currentTenantId()
                            .orElseGet(() -> {
                                if (TenantContextHolder.current().map(ctx -> ctx.systemContext()).orElse(false)) {
                                    return properties.getIngestion().getDefaultTenantId();
                                }
                                if (properties.getTenant().isAllowDemoTenantFallback()) {
                                    return properties.getIngestion().getDefaultTenantId();
                                }
                                throw new MissingTenantContextException("Tenant context is required for tenant-owned SQL");
                            });
                    return new LongValue(tenantId);
                }

                @Override
                public String getTenantIdColumn() {
                    return "tenant_id";
                }

                @Override
                public boolean ignoreTable(String tableName) {
                    if (TenantContextHolder.isBypass()) {
                        return true;
                    }
                    if (tableName == null) {
                        return false;
                    }
                    Set<String> ignored = properties.getTenant().getMybatisIgnoreTables()
                            .stream()
                            .map(value -> value.toLowerCase(Locale.ROOT))
                            .collect(Collectors.toSet());
                    return ignored.contains(tableName.toLowerCase(Locale.ROOT));
                }
            }));
        }
        return interceptor;
    }
}

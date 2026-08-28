package cc.ivera.ragdemo.service.vector;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.tenant.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DynamicMilvusStoreManagerTest {

    @Test
    void tenantScopedConfigUsesCurrentTenantCollection() {
        RagProperties properties = new RagProperties();
        MilvusStoreConfig baseConfig = MilvusStoreConfig.builder()
                .alias("default")
                .host("127.0.0.1")
                .port(19530)
                .collection("demo-kb")
                .topK(6)
                .minScore(0.55)
                .build();
        DynamicMilvusStoreManager manager = new DynamicMilvusStoreManager(
                properties,
                mock(StringRedisTemplate.class),
                new TenantMilvusCollectionResolver()
        );

        TenantContextHolder.runWith(context(9L), () -> {
            MilvusStoreConfig scoped = manager.tenantScopedConfig(baseConfig);

            assertThat(scoped.getAlias()).isEqualTo("default");
            assertThat(scoped.getCollection()).isEqualTo("demo_kb_tenant_9");
            assertThat(baseConfig.getCollection()).isEqualTo("demo-kb");
        });
    }

    private static TenantContext context(Long tenantId) {
        return new TenantContext(
                tenantId,
                "tenant-" + tenantId,
                new UserContext("user-" + tenantId, "User " + tenantId, List.of("TENANT_ADMIN"), List.of(), List.of(), List.of()),
                "test-request",
                "127.0.0.1",
                false,
                false,
                tenantId,
                null,
                Instant.now()
        );
    }
}

package cc.ivera.ragdemo.service.vector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMilvusCollectionResolverTest {

    private final TenantMilvusCollectionResolver resolver = new TenantMilvusCollectionResolver();

    @Test
    void appendsTenantSuffixAndSanitizesBaseCollectionName() {
        assertThat(resolver.collectionForTenant("demo-kb", 7L))
                .isEqualTo("demo_kb_tenant_7");
    }

    @Test
    void keepsSystemCollectionWithoutTenantSuffix() {
        assertThat(resolver.collectionForTenant("demo_kb", null)).isEqualTo("demo_kb");
        assertThat(resolver.collectionForTenant("demo_kb", 0L)).isEqualTo("demo_kb");
    }

    @Test
    void keepsCollectionNameWithinMilvusLengthLimit() {
        String longBase = "a".repeat(260);

        String collection = resolver.collectionForTenant(longBase, 123L);

        assertThat(collection).endsWith("_tenant_123");
        assertThat(collection).hasSizeLessThanOrEqualTo(255);
    }
}

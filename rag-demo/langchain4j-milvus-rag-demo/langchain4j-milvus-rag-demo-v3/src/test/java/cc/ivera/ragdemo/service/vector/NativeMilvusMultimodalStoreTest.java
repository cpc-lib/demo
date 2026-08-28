package cc.ivera.ragdemo.service.vector;

import cc.ivera.ragdemo.config.RagProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NativeMilvusMultimodalStoreTest {

    @Test
    void multimodalCollectionUsesTenantSuffix() {
        RagProperties properties = new RagProperties();
        properties.getMilvus().setMultimodalCollection("rag-mm");
        NativeMilvusMultimodalStore store = new NativeMilvusMultimodalStore(
                properties,
                new TenantMilvusCollectionResolver()
        );

        assertThat(store.collectionName(7L)).isEqualTo("rag_mm_tenant_7");
    }
}

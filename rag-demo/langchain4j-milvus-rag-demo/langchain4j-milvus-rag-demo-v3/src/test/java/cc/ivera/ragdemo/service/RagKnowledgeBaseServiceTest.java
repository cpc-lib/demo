package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.controller.RagKnowledgeBaseCreateRequest;
import cc.ivera.ragdemo.domain.rag.RagKnowledgeBase;
import cc.ivera.ragdemo.mapper.RagKnowledgeBaseMapper;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.service.vector.TenantMilvusCollectionResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagKnowledgeBaseServiceTest {

    @Test
    void createStoresTenantScopedMilvusCollectionInKnowledgeBaseMetadata() {
        RagKnowledgeBaseMapper mapper = mock(RagKnowledgeBaseMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        RagProperties properties = new RagProperties();
        properties.getMilvus().setCollection("demo_kb");
        RagKnowledgeBaseService service = new RagKnowledgeBaseService(
                mapper,
                properties,
                new TenantMilvusCollectionResolver()
        );

        service.create(new RagKnowledgeBaseCreateRequest(7L, "sales_kb", "Sales KB", "demo"));

        ArgumentCaptor<RagKnowledgeBase> captor = ArgumentCaptor.forClass(RagKnowledgeBase.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getVectorCollection()).isEqualTo("demo_kb_tenant_7");
    }

    @Test
    void pageCreatesTenantDefaultKnowledgeBaseWhenNoRowsExist() {
        RagKnowledgeBaseMapper mapper = mock(RagKnowledgeBaseMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        RagProperties properties = new RagProperties();
        properties.getMilvus().setCollection("demo_kb");
        properties.getIngestion().setDefaultKnowledgeBaseCode("default");
        properties.getIngestion().setDefaultKnowledgeBaseName("Default Knowledge Base");
        RagKnowledgeBaseService service = new RagKnowledgeBaseService(
                mapper,
                properties,
                new TenantMilvusCollectionResolver()
        );

        PageResponse<RagKnowledgeBase> page = service.page(1L, PageQuery.of(1, 50, 50, 500));

        ArgumentCaptor<RagKnowledgeBase> captor = ArgumentCaptor.forClass(RagKnowledgeBase.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getKbCode()).isEqualTo("default");
        assertThat(captor.getValue().getVectorCollection()).isEqualTo("demo_kb_tenant_1");
        assertThat(page.records()).isEmpty();
    }
}

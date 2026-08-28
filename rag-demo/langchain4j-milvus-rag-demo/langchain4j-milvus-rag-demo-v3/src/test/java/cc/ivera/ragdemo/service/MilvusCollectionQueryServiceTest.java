package cc.ivera.ragdemo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cc.ivera.ragdemo.service.vector.TenantMilvusCollectionResolver;
import cc.ivera.ragdemo.tenant.TenantContext;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import cc.ivera.ragdemo.tenant.UserContext;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.response.ListCollectionsResp;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MilvusCollectionQueryServiceTest {

    @Test
    void describeCollectionRejectsUndefinedCollectionNameBeforeCallingMilvus() throws Exception {
        MilvusClientV2 milvusClient = mock(MilvusClientV2.class);
        MilvusCollectionQueryService service = new MilvusCollectionQueryService(
                milvusClient,
                new ObjectMapper(),
                new TenantMilvusCollectionResolver()
        );

        assertThatThrownBy(() -> service.describeCollection("default", "undefined"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collectionName");

        verify(milvusClient, never()).describeCollection(any(DescribeCollectionReq.class));
    }

    @Test
    void listCollectionsOnlyReturnsCurrentTenantCollectionsForTenantUser() throws Exception {
        MilvusClientV2 milvusClient = mock(MilvusClientV2.class);
        when(milvusClient.listCollections()).thenReturn(ListCollectionsResp.builder()
                .collectionNames(List.of("demo_kb_tenant_7", "demo_kb_tenant_8", "shared_global"))
                .build());
        MilvusCollectionQueryService service = new MilvusCollectionQueryService(
                milvusClient,
                new ObjectMapper(),
                new TenantMilvusCollectionResolver()
        );

        TenantContextHolder.runWith(context(7L, List.of("TENANT_ADMIN")), () -> {
            try {
                assertThat(service.listCollections("default")).containsExactly("demo_kb_tenant_7");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    private static TenantContext context(Long tenantId, List<String> roles) {
        return new TenantContext(
                tenantId,
                "tenant-" + tenantId,
                new UserContext("user-" + tenantId, "User " + tenantId, roles, List.of(), List.of(), List.of()),
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

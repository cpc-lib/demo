package cc.ivera.ragdemo.admin;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagDocumentChunk;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.mapper.RagDocumentChunkMapper;
import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import cc.ivera.ragdemo.service.vector.MilvusStoreConfig;
import cc.ivera.ragdemo.service.vector.MultimodalVectorStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MilvusTenantDeletionWorkerTest {

    @SuppressWarnings("unchecked")
    @Test
    void deletesTextVectorsFromRecordedTenantCollection() {
        RagDocumentChunkMapper chunkMapper = mock(RagDocumentChunkMapper.class);
        DynamicMilvusStoreManager dynamicMilvusStoreManager = mock(DynamicMilvusStoreManager.class);
        ObjectProvider<MultimodalVectorStore> multimodalVectorStore = mock(ObjectProvider.class);
        MultimodalVectorStore multimodal = mock(MultimodalVectorStore.class);
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        when(chunkMapper.selectList(any())).thenReturn(List.of(chunk()));
        when(dynamicMilvusStoreManager.context("default", "demo_kb_tenant_7")).thenReturn(new ActiveMilvusContext(
                "default",
                MilvusStoreConfig.builder()
                        .alias("default")
                        .host("127.0.0.1")
                        .port(19530)
                        .collection("demo_kb_tenant_7")
                        .topK(6)
                        .minScore(0.55)
                        .build(),
                embeddingStore
        ));
        when(multimodalVectorStore.getIfAvailable()).thenReturn(multimodal);
        MilvusTenantDeletionWorker worker = new MilvusTenantDeletionWorker(
                new RagProperties(),
                chunkMapper,
                dynamicMilvusStoreManager,
                multimodalVectorStore,
                new ObjectMapper()
        );
        TenantDataDeletionTask task = new TenantDataDeletionTask();
        task.setTenantId(7L);

        worker.execute(task);

        verify(dynamicMilvusStoreManager).context("default", "demo_kb_tenant_7");
        verify(embeddingStore).removeAll(List.of("vector-1", "vector-2"));
        verify(multimodal).deleteByIds(7L, List.of("image-vector-1"));
    }

    private static RagDocumentChunk chunk() {
        RagDocumentChunk chunk = new RagDocumentChunk();
        chunk.setTenantId(7L);
        chunk.setMilvusAlias("default");
        chunk.setVectorCollection("demo_kb_tenant_7");
        chunk.setVectorId("vector-1");
        chunk.setTextVectorIds("[\"vector-2\"]");
        chunk.setImageVectorIds("[\"image-vector-1\"]");
        chunk.setIsDeleted(0);
        return chunk;
    }
}

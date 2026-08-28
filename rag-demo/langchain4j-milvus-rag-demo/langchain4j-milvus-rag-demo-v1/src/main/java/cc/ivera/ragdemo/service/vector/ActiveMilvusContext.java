package cc.ivera.ragdemo.service.vector;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

public record ActiveMilvusContext(
        String alias,
        MilvusStoreConfig config,
        EmbeddingStore<TextSegment> store
) {
}

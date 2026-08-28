package cc.ivera.ragdemo.service.rag;

import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Retriever {

    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    public List<Content> retrieve(String question) {
        ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();

        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(activeMilvus.store())
                .embeddingModel(embeddingModel)
                .maxResults(activeMilvus.config().getTopK())
                .minScore(activeMilvus.config().getMinScore())
                .build();

        return retriever.retrieve(Query.from(question));
    }
}

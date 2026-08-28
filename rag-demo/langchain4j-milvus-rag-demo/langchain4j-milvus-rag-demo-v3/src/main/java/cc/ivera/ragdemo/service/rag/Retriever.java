package cc.ivera.ragdemo.service.rag;


import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.service.query.RetrievalFilterBuilder;
import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class Retriever {

    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;
    private final RetrievalFilterBuilder retrievalFilterBuilder;

    public List<Content> retrieve(String question) {
        ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
        RagRetrievalCriteria criteria = new RagRetrievalCriteria(
                question,
                TenantContextHolder.currentTenantId().orElse(0L),
                TenantContextHolder.current().map(context -> context.user().authorizedKnowledgeBaseIds()).orElse(List.of()),
                "vector",
                activeMilvus.config().getTopK(),
                activeMilvus.config().getMinScore(),
                List.of(),
                TenantContextHolder.current().map(context -> context.user().permissionTags()).orElse(List.of())
        );
        return search(criteria).stream()
                .map(match -> Content.from(match.embedded()))
                .toList();
    }

    public List<EmbeddingMatch<TextSegment>> search(RagRetrievalCriteria criteria) {
        ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
        int maxResults = criteria.topK() == null ? activeMilvus.config().getTopK() : criteria.topK();
        double minScore = criteria.minScore() == null ? activeMilvus.config().getMinScore() : criteria.minScore();

        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder requestBuilder = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(criteria.query()).content())
                .maxResults(maxResults)
                .minScore(minScore);

        Optional<Filter> filter = retrievalFilterBuilder.buildMilvusFilter(criteria);
        filter.ifPresent(requestBuilder::filter);

        return activeMilvus.store()
                .search(requestBuilder.build())
                .matches()
                .stream()
                .filter(match -> retrievalFilterBuilder.permissionsMatch(metadataOf(match.embedded()), criteria.permissionTags()))
                .limit(maxResults)
                .toList();
    }

    private Map<String, Object> metadataOf(TextSegment segment) {
        if (segment == null || segment.metadata() == null) {
            return Map.of();
        }
        Metadata metadata = segment.metadata();
        return metadata.toMap();
    }
}

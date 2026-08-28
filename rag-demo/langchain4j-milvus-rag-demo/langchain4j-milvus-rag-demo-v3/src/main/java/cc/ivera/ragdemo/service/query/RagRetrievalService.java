package cc.ivera.ragdemo.service.query;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.service.rag.Retriever;
import cc.ivera.ragdemo.service.ragops.MultimodalRetrievalFusionPolicy;
import cc.ivera.ragdemo.service.ragops.RetrievalFusionPolicy;
import cc.ivera.ragdemo.service.ragops.RetrievalModePolicy;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RagRetrievalService {

    private final Retriever retriever;
    private final RagSearchItemConverter searchItemConverter;
    private final KeywordChunkSearchService keywordChunkSearchService;
    private final RetrievalModePolicy retrievalModePolicy;
    private final RagReranker reranker;
    private final RagProperties ragProperties;
    private final RetrievalFusionPolicy fusionPolicy;
    private final MultimodalRetrievalFusionPolicy multimodalFusionPolicy;
    private final ImageVectorRetrievalService imageVectorRetrievalService;
    private final ImageQueryTextExtractionService imageQueryTextExtractionService;

    @Autowired
    public RagRetrievalService(Retriever retriever,
                               RagSearchItemConverter searchItemConverter,
                               KeywordChunkSearchService keywordChunkSearchService,
                               RetrievalModePolicy retrievalModePolicy,
                               RagReranker reranker,
                               RagProperties ragProperties,
                               RetrievalFusionPolicy fusionPolicy,
                               ImageVectorRetrievalService imageVectorRetrievalService,
                               ImageQueryTextExtractionService imageQueryTextExtractionService) {
        this.retriever = retriever;
        this.searchItemConverter = searchItemConverter;
        this.keywordChunkSearchService = keywordChunkSearchService;
        this.retrievalModePolicy = retrievalModePolicy;
        this.reranker = reranker;
        this.ragProperties = ragProperties;
        this.fusionPolicy = fusionPolicy;
        this.multimodalFusionPolicy = new MultimodalRetrievalFusionPolicy();
        this.imageVectorRetrievalService = imageVectorRetrievalService;
        this.imageQueryTextExtractionService = imageQueryTextExtractionService;
    }

    public RagRetrievalService(Retriever retriever,
                               RagSearchItemConverter searchItemConverter,
                               KeywordChunkSearchService keywordChunkSearchService,
                               RetrievalModePolicy retrievalModePolicy,
                               RagReranker reranker,
                               RagProperties ragProperties,
                               RetrievalFusionPolicy fusionPolicy) {
        this(retriever, searchItemConverter, keywordChunkSearchService, retrievalModePolicy, reranker, ragProperties,
                fusionPolicy, null, null);
    }

    public List<RagSearchItem> retrieve(RagRetrievalCriteria criteria) {
        RetrievalModePolicy.Mode mode = retrievalModePolicy.normalize(criteria.retrievalMode());
        return switch (mode) {
            case VECTOR -> vectorSearch(criteria);
            case KEYWORD -> keywordChunkSearchService.search(criteria);
            case HYBRID -> hybridSearch(criteria);
        };
    }

    private List<RagSearchItem> vectorSearch(RagRetrievalCriteria criteria) {
        if (!hasImageInput(criteria)) {
            return textVectorSearch(criteria);
        }
        if (!imageVectorAvailable()) {
            return imageTextFallbackSearch(criteria, RetrievalModePolicy.Mode.VECTOR, null);
        }
        boolean hasQuery = hasText(criteria.query());
        List<RagSearchItem> imageItems;
        try {
            imageItems = imageVectorRetrievalService.search(criteria);
        } catch (IllegalStateException ex) {
            return imageTextFallbackSearch(criteria, RetrievalModePolicy.Mode.VECTOR, ex);
        }
        if (!hasQuery) {
            return searchItemConverter.rerank(imageItems);
        }

        int topK = retrievalModePolicy.safeTopK(criteria.topK());
        Map<String, Double> weights = requestAwareWeights(criteria, false);
        return reranker.rerank(criteria, List.of(
                new RagRetrievalResultSet("text_vector", weight(weights, "text_vector"), textVectorSearch(criteria)),
                new RagRetrievalResultSet("image_vector", weight(weights, "image_vector"), imageItems)
        ), topK);
    }

    private List<RagSearchItem> hybridSearch(RagRetrievalCriteria criteria) {
        int topK = retrievalModePolicy.safeTopK(criteria.topK());
        if (hasImageInput(criteria)) {
            if (!imageVectorAvailable()) {
                return imageTextFallbackSearch(criteria, RetrievalModePolicy.Mode.HYBRID, null);
            }
            boolean imageOnly = !hasText(criteria.query());
            Map<String, Double> weights = requestAwareWeights(criteria, imageOnly);
            List<RagRetrievalResultSet> resultSets = new java.util.ArrayList<>();
            if (hasText(criteria.query())) {
                resultSets.add(new RagRetrievalResultSet("text_vector", weight(weights, "text_vector"), textVectorSearch(criteria)));
                resultSets.add(new RagRetrievalResultSet("keyword", weight(weights, "keyword"), keywordChunkSearchService.search(criteria)));
            }
            List<RagSearchItem> imageItems;
            try {
                imageItems = imageVectorRetrievalService.search(criteria);
            } catch (IllegalStateException ex) {
                return imageTextFallbackSearch(criteria, RetrievalModePolicy.Mode.HYBRID, ex);
            }
            resultSets.add(new RagRetrievalResultSet("image_vector", weight(weights, "image_vector"), imageItems));
            return reranker.rerank(criteria, resultSets, topK);
        }
        RagProperties.Retrieval retrieval = ragProperties.getRetrieval();
        return reranker.rerank(criteria, List.of(
                new RagRetrievalResultSet("vector", fusionPolicy.vectorWeight(retrieval), textVectorSearch(criteria)),
                new RagRetrievalResultSet("keyword", fusionPolicy.keywordWeight(retrieval), keywordChunkSearchService.search(criteria))
        ), topK);
    }

    private List<RagSearchItem> textVectorSearch(RagRetrievalCriteria criteria) {
        if (!hasText(criteria.query())) {
            return List.of();
        }
        List<EmbeddingMatch<TextSegment>> matches = retriever.search(criteria);
        return searchItemConverter.fromMatches(matches);
    }

    private Map<String, Double> requestAwareWeights(RagRetrievalCriteria criteria, boolean imageOnly) {
        Map<String, Double> weights = new java.util.LinkedHashMap<>(
                multimodalFusionPolicy.weights(ragProperties.getRetrieval(), imageOnly)
        );
        putIfPresent(weights, "text_vector", criteria.textVectorWeight());
        putIfPresent(weights, "image_vector", criteria.imageVectorWeight());
        putIfPresent(weights, "keyword", criteria.keywordWeight());
        return normalize(weights);
    }

    private void putIfPresent(Map<String, Double> weights, String key, Double value) {
        if (value != null && value > 0 && !Double.isNaN(value) && !Double.isInfinite(value)) {
            weights.put(key, value);
        }
    }

    private Map<String, Double> normalize(Map<String, Double> weights) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            return weights;
        }
        Map<String, Double> normalized = new java.util.LinkedHashMap<>();
        weights.forEach((key, value) -> normalized.put(key, value / total));
        return normalized;
    }

    private double weight(Map<String, Double> weights, String key) {
        return multimodalFusionPolicy.weight(weights, key);
    }

    private List<RagSearchItem> imageTextFallbackSearch(RagRetrievalCriteria criteria,
                                                        RetrievalModePolicy.Mode fallbackMode,
                                                        IllegalStateException cause) {
        if (imageQueryTextExtractionService == null) {
            if (cause != null) {
                throw cause;
            }
            throw new IllegalStateException("Native multimodal image vector retrieval is disabled");
        }
        return imageQueryTextExtractionService.extractQueryText(criteria)
                .map(queryText -> {
                    RagRetrievalCriteria textCriteria = textOnlyCriteria(criteria, queryText, fallbackMode);
                    return switch (fallbackMode) {
                        case KEYWORD -> keywordChunkSearchService.search(textCriteria);
                        case HYBRID -> hybridSearch(textCriteria);
                        case VECTOR -> textVectorSearch(textCriteria);
                    };
                })
                .orElseGet(() -> {
                    if (cause != null) {
                        throw cause;
                    }
                    throw new IllegalStateException("Native multimodal image vector retrieval is disabled and image text extraction produced no query text");
                });
    }

    private RagRetrievalCriteria textOnlyCriteria(RagRetrievalCriteria criteria,
                                                  String queryText,
                                                  RetrievalModePolicy.Mode fallbackMode) {
        return new RagRetrievalCriteria(
                queryText,
                null,
                null,
                null,
                List.of("text"),
                criteria.tenantId(),
                criteria.knowledgeBaseIds(),
                fallbackMode.name().toLowerCase(),
                criteria.topK(),
                criteria.minScore(),
                criteria.textVectorWeight(),
                criteria.imageVectorWeight(),
                criteria.keywordWeight(),
                criteria.includeReviewPending(),
                textFallbackContentTypes(criteria.contentTypes()),
                criteria.permissionTags()
        );
    }

    private List<String> textFallbackContentTypes(List<String> originalContentTypes) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("text");
        if (originalContentTypes != null) {
            originalContentTypes.stream()
                    .filter(this::hasText)
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .forEach(values::add);
        }
        return List.copyOf(values);
    }

    private boolean imageVectorAvailable() {
        return imageVectorRetrievalService != null && imageVectorRetrievalService.enabled();
    }

    private boolean hasImageInput(RagRetrievalCriteria criteria) {
        if (criteria == null) {
            return false;
        }
        if (imageVectorRetrievalService != null) {
            return imageVectorRetrievalService.hasImageInput(criteria);
        }
        return criteria.imageAssetId() != null || hasText(criteria.imageUrl()) || hasText(criteria.imageBase64());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

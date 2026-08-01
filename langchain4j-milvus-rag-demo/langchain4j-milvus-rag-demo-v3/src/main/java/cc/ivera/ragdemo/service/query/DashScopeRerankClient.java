package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.query.RagRetrievalCriteria;
import cc.ivera.ragdemo.model.query.RagSearchItem;
import cc.ivera.ragdemo.service.MetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
public class DashScopeRerankClient implements ExternalRerankClient {

    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;
    private final HttpTransport httpTransport;
    private final MetricsService metricsService;
    private RerankObservationService observationService;

    @Autowired
    public DashScopeRerankClient(RagProperties ragProperties, ObjectMapper objectMapper, MetricsService metricsService) {
        this(ragProperties, objectMapper, new JdkHttpTransport(), metricsService);
    }

    DashScopeRerankClient(RagProperties ragProperties, ObjectMapper objectMapper, HttpTransport httpTransport, MetricsService metricsService) {
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
        this.httpTransport = httpTransport;
        this.metricsService = metricsService;
    }

    @Autowired(required = false)
    public void setObservationService(RerankObservationService observationService) {
        this.observationService = observationService;
    }

    @Override
    public boolean enabled() {
        RagProperties.Retrieval retrieval = ragProperties.getRetrieval();
        return retrieval.isRerankEnabled()
                && "dashscope".equalsIgnoreCase(retrieval.getRerankProvider())
                && StringUtils.hasText(retrieval.getRerankApiKey())
                && StringUtils.hasText(retrieval.getRerankModel())
                && StringUtils.hasText(retrieval.getRerankBaseUrl());
    }

    @Override
    public List<RagSearchItem> rerank(RagRetrievalCriteria criteria, List<RagSearchItem> candidates, int topK) {
        if (!enabled() || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates.stream().limit(Math.max(1, topK)).toList();
        }
        long started = System.nanoTime();
        int safeTopK = Math.max(1, Math.min(topK, candidates.size()));
        int estimatedInputTokens = estimateInputTokens(criteria.query(), candidates);
        try {
            RagProperties.Retrieval retrieval = ragProperties.getRetrieval();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", retrieval.getRerankModel());
            payload.put("query", criteria.query());
            payload.put("documents", candidates.stream().map(this::documentText).toList());
            payload.put("top_n", safeTopK);
            payload.put("return_documents", false);

            String response = httpTransport.postJson(
                    rerankUrl(retrieval.getRerankBaseUrl()),
                    retrieval.getRerankApiKey(),
                    objectMapper.writeValueAsString(payload),
                    Duration.ofSeconds(retrieval.getRerankTimeoutSeconds() == null ? 20 : retrieval.getRerankTimeoutSeconds())
            );
            List<RerankScore> scores = parseScores(response);
            Usage usage = parseUsage(response, estimatedInputTokens);
            if (scores.isEmpty()) {
                long latencyMs = elapsedMillis(started);
                recordObservation(criteria, candidates.size(), safeTopK, usage, latencyMs,
                        true, true, "EMPTY_RERANK_RESULT", "DashScope rerank returned no scores");
                metricsService.recordRerank(latencyMs, true);
                return candidates.stream().limit(safeTopK).toList();
            }
            List<RagSearchItem> reranked = scores.stream()
                    .filter(score -> score.index() >= 0 && score.index() < candidates.size())
                    .sorted(Comparator.comparingDouble(RerankScore::score).reversed())
                    .limit(safeTopK)
                    .map(score -> withRerankMetadata(candidates.get(score.index()), score))
                    .toList();
            long latencyMs = elapsedMillis(started);
            recordObservation(criteria, candidates.size(), safeTopK, usage, latencyMs,
                    true, false, null, null);
            metricsService.recordRerank(latencyMs, true);
            return rerankRanks(reranked);
        } catch (Exception e) {
            long latencyMs = elapsedMillis(started);
            recordObservation(criteria, candidates.size(), safeTopK,
                    new Usage(estimatedInputTokens, 0, estimatedInputTokens),
                    latencyMs,
                    false,
                    true,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            metricsService.recordRerank(latencyMs, false);
            return candidates.stream().limit(safeTopK).toList();
        }
    }

    private List<RagSearchItem> rerankRanks(List<RagSearchItem> items) {
        List<RagSearchItem> ranked = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            RagSearchItem item = items.get(i);
            ranked.add(new RagSearchItem(
                    i + 1,
                    item.score(),
                    item.knowledgeBaseId(),
                    item.documentId(),
                    item.documentName(),
                    item.chunkId(),
                    item.version(),
                    item.contentType(),
                    item.pageNo(),
                    item.sectionTitle(),
                    item.imageCaption(),
                    item.imageNumber(),
                    item.imageUrl(),
                    item.content(),
                    item.metadata()
            ));
        }
        return ranked;
    }

    private List<RerankScore> parseScores(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode results = root.path("results");
        if (!results.isArray()) {
            results = root.path("output").path("results");
        }
        List<RerankScore> scores = new ArrayList<>();
        if (!results.isArray()) {
            return scores;
        }
        for (JsonNode result : results) {
            int index = result.path("index").asInt(-1);
            double score = result.has("relevance_score")
                    ? result.path("relevance_score").asDouble()
                    : result.path("score").asDouble();
            if (index >= 0) {
                scores.add(new RerankScore(index, score));
            }
        }
        return scores;
    }

    private Usage parseUsage(String response, int estimatedInputTokens) throws IOException {
        JsonNode usage = objectMapper.readTree(response).path("usage");
        int inputTokens = firstPositive(
                usage.path("input_tokens").asInt(0),
                usage.path("prompt_tokens").asInt(0),
                estimatedInputTokens
        );
        int outputTokens = firstPositive(
                usage.path("output_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0),
                0
        );
        int totalTokens = firstPositive(
                usage.path("total_tokens").asInt(0),
                inputTokens + outputTokens
        );
        return new Usage(inputTokens, outputTokens, totalTokens);
    }

    private RagSearchItem withRerankMetadata(RagSearchItem item, RerankScore rerankScore) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (item.metadata() != null) {
            metadata.putAll(item.metadata());
        }
        metadata.put("rerank_provider", "dashscope");
        metadata.put("rerank_model", ragProperties.getRetrieval().getRerankModel());
        metadata.put("rerank_score", rerankScore.score());
        metadata.put("pre_rerank_rank", item.rank());
        return new RagSearchItem(
                0,
                rerankScore.score(),
                item.knowledgeBaseId(),
                item.documentId(),
                item.documentName(),
                item.chunkId(),
                item.version(),
                item.contentType(),
                item.pageNo(),
                item.sectionTitle(),
                item.imageCaption(),
                item.imageNumber(),
                item.imageUrl(),
                item.content(),
                metadata
        );
    }

    private String documentText(RagSearchItem item) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(item.documentName())) {
            parts.add(item.documentName());
        }
        if (StringUtils.hasText(item.sectionTitle())) {
            parts.add(item.sectionTitle());
        }
        if (StringUtils.hasText(item.content())) {
            parts.add(item.content());
        }
        return String.join("\n", parts);
    }

    private int estimateInputTokens(String query, List<RagSearchItem> candidates) {
        int chars = query == null ? 0 : query.length();
        for (RagSearchItem candidate : candidates) {
            chars += documentText(candidate).length();
        }
        return Math.max(1, chars / 4);
    }

    private int firstPositive(int first, int second) {
        return first > 0 ? first : Math.max(0, second);
    }

    private int firstPositive(int first, int second, int third) {
        if (first > 0) {
            return first;
        }
        return second > 0 ? second : Math.max(0, third);
    }

    private long elapsedMillis(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }

    private void recordObservation(RagRetrievalCriteria criteria,
                                   int candidateCount,
                                   int topK,
                                   Usage usage,
                                   long latencyMs,
                                   boolean success,
                                   boolean fallback,
                                   String errorCode,
                                   String errorMessage) {
        if (observationService == null) {
            return;
        }
        RagProperties.Retrieval retrieval = ragProperties.getRetrieval();
        observationService.record(
                criteria.tenantId(),
                criteria.query(),
                retrieval.getRerankProvider(),
                retrieval.getRerankModel(),
                candidateCount,
                topK,
                usage.inputTokens(),
                usage.outputTokens(),
                usage.totalTokens(),
                latencyMs,
                success,
                fallback,
                errorCode,
                errorMessage
        );
    }

    private URI rerankUrl(String baseUrl) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String url = trimmed.endsWith("/reranks") ? trimmed : trimmed + "/reranks";
        return URI.create(url);
    }

    record RerankScore(int index, double score) {
    }

    record Usage(int inputTokens, int outputTokens, int totalTokens) {
    }

    interface HttpTransport {
        String postJson(URI uri, String apiKey, String body, Duration timeout) throws IOException, InterruptedException;
    }

    private static class JdkHttpTransport implements HttpTransport {
        private final HttpClient httpClient = HttpClient.newHttpClient();

        @Override
        public String postJson(URI uri, String apiKey, String body, Duration timeout) throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("DashScope rerank failed: HTTP " + response.statusCode());
            }
            return response.body();
        }
    }
}

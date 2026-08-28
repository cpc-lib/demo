package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.util.LogMasker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ElasticsearchTenantDeletionWorker implements TenantDeletionWorker {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String stageCode() {
        return TenantDeletionStageCode.ELASTICSEARCH.name();
    }

    @Override
    public TenantDeletionStageResult dryRun(TenantDataDeletionTask task) {
        if (!enabled()) {
            return TenantDeletionStageResult.skipped(stageCode(), "keyword-index-disabled");
        }
        long count = count(task.getTenantId());
        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("keywordDocuments", count);
            return TenantDeletionStageResult.success(stageCode(), count, objectMapper.writeValueAsString(result));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to build dryRun result JSON", e);
        }
    }

    @Override
    public TenantDeletionStageResult execute(TenantDataDeletionTask task) {
        if (!enabled()) {
            return TenantDeletionStageResult.skipped(stageCode(), "keyword-index-disabled");
        }
        if (!properties.getTenantDeletion().isElasticsearchDeleteEnabled()) {
            return TenantDeletionStageResult.skipped(stageCode(), "elasticsearch-delete-disabled");
        }
        request("POST", indexUri("_delete_by_query"), tenantQuery(task.getTenantId()), false);
        try {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("deleteByQuery", true);
            return TenantDeletionStageResult.success(stageCode(), count(task.getTenantId()), objectMapper.writeValueAsString(result));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to build execute result JSON", e);
        }
    }

    @Override
    public TenantDeletionStageResult verify(TenantDataDeletionTask task) {
        if (!enabled()) {
            return TenantDeletionStageResult.skipped(stageCode(), "keyword-index-disabled");
        }
        long remaining = count(task.getTenantId());
        if (remaining == 0 || !properties.getTenantDeletion().isElasticsearchDeleteEnabled()) {
            try {
                ObjectNode result = objectMapper.createObjectNode();
                result.put("remainingKeywordDocuments", remaining);
                return TenantDeletionStageResult.success(stageCode(), remaining, objectMapper.writeValueAsString(result));
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new RuntimeException("Failed to build verify result JSON", e);
            }
        } else {
            return TenantDeletionStageResult.failed(stageCode(), "KEYWORD_INDEX_DOCS_REMAIN", "Remaining keyword index documents: " + remaining);
        }
    }

    private long count(Long tenantId) {
        try {
            String body = request("POST", indexUri("_count"), tenantQuery(tenantId), true);
            JsonNode root = objectMapper.readTree(body);
            return root.path("count").asLong(0);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to count tenant keyword index documents", ex);
        }
    }

    private String tenantQuery(Long tenantId) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode bool = objectMapper.createObjectNode();
        ArrayNode filter = objectMapper.createArrayNode();
        ObjectNode term = objectMapper.createObjectNode();
        ObjectNode tenantIdTerm = objectMapper.createObjectNode();
        tenantIdTerm.put("tenantId", tenantId != null ? tenantId : 0);
        term.set("term", tenantIdTerm);
        filter.add(term);
        bool.set("filter", filter);
        query.set("bool", bool);
        root.set("query", query);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to build tenant query JSON", e);
        }
    }

    private String request(String method, URI uri, String body, boolean allowNotFound) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(timeoutSeconds()))
                    .header("Content-Type", "application/json");
            headers().forEach(builder::header);
            builder.method(method, StringUtils.hasText(body)
                    ? HttpRequest.BodyPublishers.ofString(body)
                    : HttpRequest.BodyPublishers.noBody());
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (!allowNotFound && (response.statusCode() < 200 || response.statusCode() >= 300)) {
                throw new IOException("HTTP " + response.statusCode() + ": " + LogMasker.truncateAndMask(response.body()));
            }
            if (response.statusCode() == 404) {
                return "{}";
            }
            return response.body();
        } catch (IOException ex) {
            throw new IllegalStateException("Elasticsearch request failed: " + uri, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch request interrupted", ex);
        }
    }

    private Map<String, String> headers() {
        RagProperties.KeywordIndex config = properties.getKeywordIndex();
        Map<String, String> headers = new LinkedHashMap<>();
        if (StringUtils.hasText(config.getApiKey())) {
            headers.put("Authorization", "ApiKey " + config.getApiKey());
        } else if (StringUtils.hasText(config.getUsername()) && StringUtils.hasText(config.getPassword())) {
            String credentials = config.getUsername() + ":" + config.getPassword();
            headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        return headers;
    }

    private URI indexUri(String suffix) {
        RagProperties.KeywordIndex config = properties.getKeywordIndex();
        String index = StringUtils.hasText(config.getIndexAlias()) ? config.getIndexAlias() : config.getIndexName();
        return URI.create(trimTrailingSlash(config.getBaseUrl()) + "/" + pathSegment(index) + "/" + suffix);
    }

    private boolean enabled() {
        RagProperties.KeywordIndex config = properties.getKeywordIndex();
        return config.isEnabled() && StringUtils.hasText(config.getBaseUrl()) && StringUtils.hasText(config.getIndexName());
    }

    private int timeoutSeconds() {
        Integer seconds = properties.getKeywordIndex().getTimeoutSeconds();
        return seconds == null || seconds < 1 ? 10 : seconds;
    }

    private String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

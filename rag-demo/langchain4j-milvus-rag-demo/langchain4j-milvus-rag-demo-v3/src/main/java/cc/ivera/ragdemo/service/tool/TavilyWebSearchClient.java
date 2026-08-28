package cc.ivera.ragdemo.service.tool;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.SourceType;
import cc.ivera.ragdemo.util.LogMasker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TavilyWebSearchClient {

    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public boolean isEnabled() {
        return properties.getWebSearch().isEnabled()
                && StringUtils.hasText(properties.getWebSearch().getTavilyApiKey());
    }

    public List<SourceItem> searchTop10(String query) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getWebSearch().getTimeoutSeconds()))
                    .build();

            JsonNode body = objectMapper.createObjectNode()
                    .put("query", query)
                    .put("max_results", properties.getWebSearch().getMaxResults())
                    .put("search_depth", properties.getWebSearch().getSearchDepth());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getWebSearch().getTavilyBaseUrl() + "/search"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getWebSearch().getTavilyApiKey())
                    .timeout(Duration.ofSeconds(properties.getWebSearch().getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Tavily search failed, status=" + response.statusCode() + ", body=" + LogMasker.truncateAndMask(response.body()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");
            List<SourceItem> items = new ArrayList<>();
            if (results.isArray()) {
                for (JsonNode item : results) {
                    items.add(SourceItem.builder()
                            .type(SourceType.WEB)
                            .title(item.path("title").asText("未命名来源"))
                            .url(item.path("url").asText(null))
                            .content(item.path("content").asText(""))
                            .score(item.hasNonNull("score") ? item.path("score").asDouble() : null)
                            .build());
                }
            }
            return items;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("互联网搜索失败: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("互联网搜索失败: " + e.getMessage(), e);
        }
    }
}

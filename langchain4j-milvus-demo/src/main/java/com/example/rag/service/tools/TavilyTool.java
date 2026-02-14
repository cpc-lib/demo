package com.example.rag.service.tools;

import com.example.rag.config.AppProperties;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Tavily web search tool (function calling).
 */
public class TavilyTool {

    private final WebClient client;
    private final AppProperties props;

    public TavilyTool(String apiKey, String baseUrl, AppProperties props) {
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.props = props;
    }

    @Tool("Use Tavily to search the web for up-to-date information.")
    public String webSearch(String query) {
        Map<String, Object> body = Map.of(
                "query", query,
                "search_depth", "basic",
                "max_results", props.getTavily().getMaxResults()
        );

        return client.post()
                .uri("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}

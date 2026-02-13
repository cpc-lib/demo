package com.example.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

public class TavilySearchTool {

  private final WebClient client;

  public TavilySearchTool(String apiKey, String baseUrl) {
    this.client = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .build();
  }

  @Tool("Use Tavily to search the web for up-to-date information.")
  public String webSearch(String query) {
    Map<String, Object> body = Map.of(
        "query", query,
        "search_depth", "basic",
        "max_results", 20
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

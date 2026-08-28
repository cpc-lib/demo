package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.agent.AgentAssistant;
import cc.ivera.ragdemo.service.tool.KnowledgeTool;
import cc.ivera.ragdemo.service.tool.TicketTool;
import cc.ivera.ragdemo.service.tool.TextToImageTool;
import cc.ivera.ragdemo.service.tool.WebSearchTool;
import cc.ivera.ragdemo.service.tool.WeatherTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
@RequiredArgsConstructor
@Slf4j
public class AiConfig {

    private final RagProperties props;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(props.getEmbedding().getBaseUrl())
                .apiKey(props.getEmbedding().getApiKey())
                .modelName(props.getEmbedding().getModel())
                .build();
    }

    @Bean
    public ChatLanguageModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(props.getLlm().getBaseUrl())
                .apiKey(props.getLlm().getApiKey())
                .modelName(props.getLlm().getModel())
                .temperature(props.getLlm().getTemperature())
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(props.getLlm().getBaseUrl())
                .apiKey(props.getLlm().getApiKey())
                .modelName(props.getLlm().getModel())
                .temperature(props.getLlm().getTemperature())
                .build();
    }


    @Bean
    public AgentAssistant agentAssistant(ChatLanguageModel chatLanguageModel,
                                         KnowledgeTool knowledgeTool,
                                         WebSearchTool webSearchTool,
                                         WeatherTool weatherTool,
                                         TicketTool ticketTool,
                                         TextToImageTool textToImageTool) {
        List<Object> configuredTools = resolveAgentTools(knowledgeTool, webSearchTool, weatherTool, ticketTool, textToImageTool);
        return AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(props.getAgent().getMemoryMaxMessages()))
                .tools(configuredTools.toArray())
                .build();
    }

    private List<Object> resolveAgentTools(KnowledgeTool knowledgeTool,
                                           WebSearchTool webSearchTool,
                                           WeatherTool weatherTool,
                                           TicketTool ticketTool,
                                           TextToImageTool textToImageTool) {
        Map<String, Object> availableTools = new LinkedHashMap<>();
        availableTools.put("ticket", ticketTool);
        availableTools.put("knowledge", knowledgeTool);
        if (props.getWebSearch().isEnabled()) {
            availableTools.put("web-search", webSearchTool);
        }
        if (props.getWeather().isEnabled()) {
            availableTools.put("weather", weatherTool);
        }
        if (props.getImage().isEnabled()) {
            availableTools.put("text-to-image", textToImageTool);
        }

        List<Object> resolved = new ArrayList<>();
        for (String rawName : props.getAgent().getTools()) {
            String name = normalizeToolName(rawName);
            Object tool = availableTools.get(name);
            if (tool == null) {
                throw new IllegalArgumentException("Unknown or disabled tool configured: " + rawName);
            }
            resolved.add(tool);
        }

        if (resolved.isEmpty()) {
            throw new IllegalArgumentException("No tools configured for rag.agent.tools");
        }

        log.info("Agent tools loaded from yml: {}", props.getAgent().getTools());
        return resolved;
    }

    private String normalizeToolName(String toolName) {
        if (toolName == null) {
            return "";
        }
        String normalized = toolName.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ticket", "tickettool", "ticket-analysis", "ticketanalysis" -> "ticket";
            case "knowledge", "knowledgetool", "knowledge-search", "knowledgesearch" -> "knowledge";
            case "web-search", "websearch", "web-search-tool", "websearchtool" -> "web-search";
            case "weather", "weathertool", "weather-forecast", "weatherforecast" -> "weather";
            case "text-to-image", "texttoimage", "image", "image-generation", "imagetool" -> "text-to-image";
            default -> normalized;
        };
    }

    @Bean
    public MilvusClientV2 milvusClient() {
        String uri = "http://" + props.getMilvus().getHost() + ":" + props.getMilvus().getPort();

        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri(uri);

        String username = props.getMilvus().getUsername();
        String password = props.getMilvus().getPassword();
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            builder.token(username.trim() + ":" + password.trim());
        }

        return new MilvusClientV2(builder.build());
    }
}

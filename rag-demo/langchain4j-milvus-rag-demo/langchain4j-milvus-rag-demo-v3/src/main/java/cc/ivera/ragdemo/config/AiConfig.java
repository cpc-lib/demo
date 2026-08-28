package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.agent.AgentAssistant;
import cc.ivera.ragdemo.service.agent.PromptService;
import cc.ivera.ragdemo.service.tenant.DynamicModelFactory;
import cc.ivera.ragdemo.service.tool.*;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.*;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
public class AiConfig {

    private final RagProperties props;
    private final DynamicModelFactory dynamicModelFactory;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // Dynamic embedding model that delegates to DynamicModelFactory
        return new EmbeddingModel() {
            @Override
            public Response<Embedding> embed(String text) {
                return dynamicModelFactory.getEmbeddingModel().embed(text);
            }

            @Override
            public Response<Embedding> embed(TextSegment textSegment) {
                return dynamicModelFactory.getEmbeddingModel().embed(textSegment);
            }

            @Override
            public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
                return dynamicModelFactory.getEmbeddingModel().embedAll(textSegments);
            }
        };
    }

    @Bean
    @Primary
    public ChatLanguageModel chatModel() {
        // Dynamic chat model that delegates to DynamicModelFactory
        return new ChatLanguageModel() {
            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages) {
                return dynamicModelFactory.getLlmModel().generate(messages);
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
                return dynamicModelFactory.getLlmModel().generate(messages, toolSpecifications);
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
                return dynamicModelFactory.getLlmModel().generate(messages, toolSpecification);
            }

            @Override
            public Response<AiMessage> generate(ChatMessage... messages) {
                return dynamicModelFactory.getLlmModel().generate(messages);
            }

            @Override
            public ChatResponse chat(ChatRequest request) {
                return dynamicModelFactory.getLlmModel().chat(request);
            }

            @Override
            public Set<Capability> supportedCapabilities() {
                return dynamicModelFactory.getLlmModel().supportedCapabilities();
            }
        };
    }

    @Bean
    @Primary
    public StreamingChatLanguageModel streamingChatModel() {
        // Dynamic streaming chat model that delegates to DynamicModelFactory
        return new StreamingChatLanguageModel() {
            @Override
            public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
                dynamicModelFactory.getStreamingLlmModel().generate(messages, handler);
            }

            @Override
            public void generate(List<ChatMessage> messages,
                                 List<ToolSpecification> toolSpecifications,
                                 StreamingResponseHandler<AiMessage> handler) {
                dynamicModelFactory.getStreamingLlmModel().generate(messages, toolSpecifications, handler);
            }

            @Override
            public void generate(List<ChatMessage> messages,
                                 ToolSpecification toolSpecification,
                                 StreamingResponseHandler<AiMessage> handler) {
                dynamicModelFactory.getStreamingLlmModel().generate(messages, toolSpecification, handler);
            }
        };
    }


    @Bean
    public AgentAssistant agentAssistant(ChatLanguageModel chatLanguageModel,
                                         KnowledgeTool knowledgeTool,
                                         WebSearchTool webSearchTool,
                                         WeatherTool weatherTool,
                                         TicketTool ticketTool,
                                         TextToImageTool textToImageTool,
                                         PromptService promptService) {
        List<Object> configuredTools = resolveAgentTools(knowledgeTool, webSearchTool, weatherTool, ticketTool, textToImageTool);
        return AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(props.getAgent().getMemoryMaxMessages()))
                .tools(configuredTools.toArray())
                .systemMessageProvider(memoryId -> promptService.getActiveSystemPrompt())
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

        RagProperties.Milvus milvusProps = props.getMilvus();

        // Authentication
        String username = milvusProps.getUsername();
        String password = milvusProps.getPassword();
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            builder.username(username.trim());
            builder.password(password.trim());
        }

        // Connection timeout
        builder.connectTimeoutMs(milvusProps.getConnectTimeoutMs());

        // RPC deadline (max duration for single RPC call)
        builder.rpcDeadlineMs(milvusProps.getRpcDeadlineMs());

        // Keep-alive settings
        builder.keepAliveTimeMs(milvusProps.getKeepAliveTimeMs());
        builder.keepAliveTimeoutMs(milvusProps.getKeepAliveTimeoutMs());
        builder.keepAliveWithoutCalls(milvusProps.isKeepAliveWithoutCalls());

        // Idle timeout (close connection after idle)
        builder.idleTimeoutMs(milvusProps.getIdleTimeoutMs());

        // TLS/secure settings
        builder.secure(milvusProps.isSecure());

        // Default database name
        if (milvusProps.getDbName() != null && !milvusProps.getDbName().isBlank()) {
            builder.dbName(milvusProps.getDbName().trim());
        }

        log.info("Creating MilvusClientV2 with URI: {}, timeout: {}ms, rpcDeadline: {}ms",
                uri, milvusProps.getConnectTimeoutMs(), milvusProps.getRpcDeadlineMs());

        return new MilvusClientV2(builder.build());
    }
}

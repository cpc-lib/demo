package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.agent.AgentAssistant;
import cc.ivera.ragdemo.service.tool.KnowledgeTool;
import cc.ivera.ragdemo.service.tool.TicketTool;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
@RequiredArgsConstructor
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
                                         TicketTool ticketTool) {
        return AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(props.getAgent().getMemoryMaxMessages()))
                .tools(ticketTool, knowledgeTool, webSearchTool, weatherTool)
                .build();
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

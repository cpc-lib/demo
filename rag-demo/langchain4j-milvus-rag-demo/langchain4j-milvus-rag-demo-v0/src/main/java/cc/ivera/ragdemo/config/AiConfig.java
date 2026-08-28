package cc.ivera.ragdemo.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

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
    @Lazy
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host(props.getMilvus().getHost())
                .port(props.getMilvus().getPort())
                .collectionName(props.getMilvus().getCollection())
                .dimension(props.getEmbedding().getDimension())
                .build();
    }
}

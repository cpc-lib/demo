package com.example.ai.config;

import com.example.ai.memory.MySqlChatMemoryStore;
import com.example.ai.rag.RagIngestor;
import com.example.ai.service.Assistant;
import com.example.ai.service.StreamAssistant;
import com.example.ai.tools.TavilySearchTool;
import com.example.ai.tools.WeatherTool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AiConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${ai.dashscope.api-key}") String apiKey,
            @Value("${ai.dashscope.base-url}") String baseUrl,
            @Value("${ai.dashscope.chat-model}") String modelName
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatModel(
            @Value("${ai.dashscope.api-key}") String apiKey,
            @Value("${ai.dashscope.base-url}") String baseUrl,
            @Value("${ai.dashscope.chat-model}") String modelName
    ) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .build();
    }

    @Bean
    public OpenAiEmbeddingModel embeddingModel(
            @Value("${ai.dashscope.api-key}") String apiKey,
            @Value("${ai.dashscope.base-url}") String baseUrl,
            @Value("${ai.dashscope.embedding-model}") String embeddingModel
    ) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModel)
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * ✅ LangChain4j 1.11.0：RAG 推荐直接使用 ContentRetriever 注入 AiServices
     * （不再依赖 RetrievalAugmentor.builder()）
     */
    @Bean
    public ContentRetriever contentRetriever(OpenAiEmbeddingModel embeddingModel,
                                             EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.45)
                .build();
    }

    @Bean
    public MySqlChatMemoryStore mySqlChatMemoryStore(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return new MySqlChatMemoryStore(jdbcTemplate);
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(MySqlChatMemoryStore store) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(30)
                .chatMemoryStore(store)
                .build();
    }

    @Bean
    public TavilySearchTool tavilySearchTool(
            @Value("${ai.tavily.api-key}") String apiKey,
            @Value("${ai.tavily.base-url}") String baseUrl
    ) {
        return new TavilySearchTool(apiKey, baseUrl);
    }

    @Bean
    public WeatherTool weatherTool(TavilySearchTool tavilySearchTool) {
        return new WeatherTool(tavilySearchTool);
    }


    @Bean
    public Assistant assistant(ChatModel chatModel,
                               ChatMemoryProvider chatMemoryProvider,
                               ContentRetriever contentRetriever,
                               TavilySearchTool tavilySearchTool,
                               WeatherTool weatherTool) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .tools(tavilySearchTool, weatherTool)
                .build();
    }

    @Bean
    public StreamAssistant streamAssistant(StreamingChatModel streamingChatModel,
                                           ChatMemoryProvider chatMemoryProvider,
                                           ContentRetriever contentRetriever,
                                           TavilySearchTool tavilySearchTool,
                                           WeatherTool weatherTool) {
        return AiServices.builder(StreamAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .tools(tavilySearchTool, weatherTool)
                .build();
    }

    @Bean
    public RagIngestor ragIngestor(OpenAiEmbeddingModel embeddingModel,
                                   EmbeddingStore<TextSegment> embeddingStore) {
        return new RagIngestor(embeddingModel, embeddingStore);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner ingestOnStartup(RagIngestor ingestor) {
        return args -> {
            List<Document> docs = ClassPathDocumentLoader.loadDocuments("rag/");
            ingestor.ingest(docs);
        };
    }
}

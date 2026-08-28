package cc.ivera.ai.config;

import cc.ivera.ai.memory.MysqlChatMemoryStore;
import cc.ivera.ai.rag.RagIngestor;
import cc.ivera.ai.service.ChatAssistant;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class LangChainConfig {

    @Bean
    public OpenAiChatModel chatModel(
            @Value("${ai.dashscope.api-key}") String apiKey,
            @Value("${ai.dashscope.base-url}") String baseUrl,
            @Value("${ai.qwen.chat-model}") String modelName,
            @Value("${ai.qwen.enable-search:true}") boolean enableSearch,
            @Value("${ai.qwen.forced-search:true}") boolean forcedSearch
    ) {
        // DashScope(OpenAI兼容) 扩展参数：enable_search + search_options
        Map<String, Object> custom = new HashMap<>();
        custom.put("enable_search", enableSearch);

        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("forced_search", forcedSearch);
        custom.put("search_options", searchOptions);

        OpenAiChatRequestParameters params = OpenAiChatRequestParameters.builder()
                .customParameters(custom)
                .build();

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)      // https://dashscope.aliyuncs.com/compatible-mode/v1
                .modelName(modelName)  // qwen-plus
                .timeout(Duration.ofSeconds(60))
                .defaultRequestParameters(params)
                .build();
    }

    @Bean
    public OpenAiEmbeddingModel embeddingModel(
            @Value("${ai.dashscope.api-key}") String apiKey,
            @Value("${ai.dashscope.base-url}") String baseUrl,
            @Value("${ai.qwen.embedding-model}") String embeddingModelName
    ) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public EmbeddingStore<?> embeddingStore() {
        // 演示用：内存向量库（生产可替换 Milvus/PGVector/ES 等）
        return new InMemoryEmbeddingStore<>();
    }


    @Bean
    public ChatAssistant chatAssistant(OpenAiChatModel chatModel,
                                       MysqlChatMemoryStore mysqlChatMemoryStore,
                                       OpenAiEmbeddingModel embeddingModel,
                                       EmbeddingStore<TextSegment> embeddingStore) {

        var retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5)
                .minScore(0.2)
                .build();

        return AiServices.builder(ChatAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(30)
                        .chatMemoryStore(mysqlChatMemoryStore)
                        .build())
                .contentRetriever(retriever) // ✅ 直接挂载
                .build();
    }


    @Bean
    public RagIngestor ragIngestor(OpenAiEmbeddingModel embeddingModel, EmbeddingStore<?> embeddingStore) {
        return new RagIngestor(embeddingModel, (EmbeddingStore) embeddingStore);
    }
}

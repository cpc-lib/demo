package com.example.rag.config;

import com.example.rag.service.ai.Assistant;
import com.example.rag.service.tools.KnowledgeTool;
import com.example.rag.service.tools.TavilyTool;
import com.example.rag.service.tools.WeatherTool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AiConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(AppProperties props) {
        return OpenAiChatModel.builder()
                .baseUrl(props.getDashscope().getBaseUrl())   // https://dashscope.aliyuncs.com/compatible-mode/v1
                .apiKey(props.getDashscope().getApiKey())
                .modelName(props.getDashscope().getModel())   // qwen-plus
                .temperature(0.2)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel(); // dim=384
    }

    // ✅ 关键修复：加上泛型 TextSegment
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(AppProperties props, EmbeddingModel embeddingModel) {
        return MilvusEmbeddingStore.builder()
                .uri(props.getMilvus().getUri())
                .collectionName(props.getMilvus().getCollection())
                // ✅ 关键修复：dimension 由 embeddingModel 决定，避免配置写错
                .dimension(embeddingModel.dimension())
                .build();
    }

    @Bean
    public WebSearchEngine tavilyWebSearchEngine(AppProperties props) {
        // ✅ 兼容：如果你当前依赖版本没有 maxResults(...) 就删掉那一行
        TavilyWebSearchEngine.TavilyWebSearchEngineBuilder builder =
                TavilyWebSearchEngine.builder()
                        .apiKey(props.getTavily().getApiKey());

        // 如果你的版本支持 maxResults，保留；不支持就注释/删除
        try {
            builder.getClass().getMethod("maxResults", int.class)
                    .invoke(builder, props.getTavily().getMaxResults());
        } catch (Exception ignore) {
            // ignore: 当前版本不支持 maxResults
        }

        return builder.build();
    }


    // ✅ 工具 bean 化（推荐）
    @Bean
    public KnowledgeTool knowledgeTool(EmbeddingModel embeddingModel,
                                       EmbeddingStore<TextSegment> embeddingStore,
                                       AppProperties props) {
        return new KnowledgeTool(embeddingModel, embeddingStore, props);
    }

    @Bean
    public TavilyTool tavilyTool(
            @Value("${app.tavily.api-key}") String apiKey,
            @Value("${app.tavily.base-url}") String baseUrl,
            AppProperties appProperties

    ) {
        return new TavilyTool(apiKey, baseUrl,appProperties);
    }

    @Bean
    public WeatherTool weatherTool(TavilyTool tavilyTool) {
        return new WeatherTool(tavilyTool);
    }

    @Bean
    public Assistant assistant(OpenAiChatModel chatLanguageModel,
                               KnowledgeTool knowledgeTool,
                               TavilyTool tavilyTool, WeatherTool weatherTool
    ) {

        return AiServices.builder(Assistant.class)
                .chatModel(chatLanguageModel)
                .tools(knowledgeTool, tavilyTool, weatherTool)
                .build();
    }
}

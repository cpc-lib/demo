package com.example.rag.service.tools;

import com.example.rag.config.AppProperties;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Vector-store retrieval tool (Milvus).
 * The LLM must call this tool first per the system policy.
 */
public class KnowledgeTool {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AppProperties props;

    public KnowledgeTool(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore, AppProperties props) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.props = props;
    }

    @Tool(name = "knowledge_search", value = "从向量库检索与 query 最相关的知识片段。返回为空表示没检索到。")
    public String search(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder().query(query).queryEmbedding(queryEmbedding).maxResults(props.getMilvus().getMaxResults()).minScore(props.getMilvus().getMinScore()).build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        if (matches == null || matches.isEmpty()) {
            return "";
        }

        return matches.stream().map(m -> String.format("score=%.3f\n%s", m.score(), m.embedded().text())).collect(Collectors.joining("\n\n---\n\n"));
    }
}

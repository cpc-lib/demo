package cc.ivera.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;

public class RagIngestor {

    private final OpenAiEmbeddingModel embeddingModel;
    private final EmbeddingStore embeddingStore;

    public RagIngestor(OpenAiEmbeddingModel embeddingModel, EmbeddingStore embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        ingestOnStartup();
    }

    private void ingestOnStartup() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:rag-docs/*.*");

            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .documentSplitter(DocumentSplitters.recursive(500, 80))
                    .build();

            for (Resource r : resources) {
                String text = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                Document doc = Document.from(text, Metadata.from("source", r.getFilename()));
                ingestor.ingest(doc);
            }
        } catch (Exception e) {
            throw new RuntimeException("RAG docs ingest failed", e);
        }
    }
}

package com.example.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

public class RagIngestor {

  private final EmbeddingModel embeddingModel;
  private final EmbeddingStore<TextSegment> embeddingStore;

  public RagIngestor(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
    this.embeddingModel = embeddingModel;
    this.embeddingStore = embeddingStore;
  }

  public void ingest(List<Document> documents) {
    var splitter = DocumentSplitters.recursive(800, 120);
    for (Document doc : documents) {
      List<TextSegment> segments = splitter.split(doc);
      var embeddings = embeddingModel.embedAll(segments).content();
      for (int i = 0; i < segments.size(); i++) {
        embeddingStore.add(embeddings.get(i), segments.get(i));
      }
    }
  }
}

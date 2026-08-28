package com.example.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
public class DocumentService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private final DocumentParser documentParser = new ApacheTikaDocumentParser(false);

    public DocumentService(EmbeddingModel embeddingModel,
                           EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public IngestResult ingest(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        try (InputStream is = file.getInputStream()) {
            Document document = documentParser.parse(is);

            // Recursive split (character-based) similar to RecursiveCharacterTextSplitter
            DocumentSplitter splitter = DocumentSplitters.recursive(800, 120);
            List<TextSegment> segments = splitter.split(document);

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

            embeddingStore.addAll(embeddings, segments);

            return new IngestResult(file.getOriginalFilename(), segments.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to ingest file: " + file.getOriginalFilename(), e);
        }
    }

    public record IngestResult(String filename, int segments) {}
}

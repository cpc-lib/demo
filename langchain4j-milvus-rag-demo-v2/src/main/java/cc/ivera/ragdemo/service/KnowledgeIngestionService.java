package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.service.ingest.Splitter;
import cc.ivera.ragdemo.service.ingest.TikaParser;
import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

@Service
@RequiredArgsConstructor
public class KnowledgeIngestionService {

    private static final int EMBED_BATCH_SIZE = 10;

    private final TikaParser tikaParser;
    private final Splitter splitter;
    private final EmbeddingModel embeddingModel;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeToMap(Object metadataObj) {
        if (metadataObj == null) {
            return Collections.emptyMap();
        }

        if (metadataObj instanceof Map<?, ?> m) {
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }

        try {
            Method toMap = metadataObj.getClass().getMethod("toMap");
            Object res = toMap.invoke(metadataObj);
            if (res instanceof Map<?, ?> m) {
                Map<String, Object> out = new HashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
                return out;
            }
        } catch (Exception ignore) {
        }

        return Collections.emptyMap();
    }

    public int ingestText(String text) {
        Document doc = Document.from(text);
        return ingestDocument(doc, Map.of("source", "text"));
    }

    public int ingestFile(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            Document doc = tikaParser.parse(in);
            return ingestDocument(doc, Map.of(
                    "source", "file",
                    "fileName", Optional.ofNullable(file.getOriginalFilename()).orElse("unknown")
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to ingest file: " + e.getMessage(), e);
        }
    }

    private int ingestDocument(Document document, Map<String, Object> extraMetadata) {
        ActiveMilvusContext activeMilvus = dynamicMilvusStoreManager.current();
        EmbeddingStore<TextSegment> embeddingStore = activeMilvus.store();

        Map<String, Object> merged = new HashMap<>(safeToMap(document.metadata()));
        merged.putAll(extraMetadata);
        merged.put("milvusAlias", activeMilvus.alias());
        merged.put("milvusCollection", activeMilvus.config().getCollection());

        Document docWithMeta = Document.from(document.text(), Metadata.from(merged));

        List<TextSegment> segments = splitter.split(docWithMeta);

        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);

            Map<String, Object> segMeta = new HashMap<>(safeToMap(seg.metadata()));
            segMeta.putAll(merged);
            segMeta.put("chunkId", i);

            segments.set(i, TextSegment.from(seg.text(), Metadata.from(segMeta)));
        }

        for (int from = 0; from < segments.size(); from += EMBED_BATCH_SIZE) {
            int to = Math.min(from + EMBED_BATCH_SIZE, segments.size());

            List<TextSegment> batchSegs = new ArrayList<>(segments.subList(from, to));

            Response<List<Embedding>> resp = embeddingModel.embedAll(batchSegs);
            List<Embedding> embeddings = resp.content();

            embeddingStore.addAll(embeddings, batchSegs);
        }

        return segments.size();
    }
}

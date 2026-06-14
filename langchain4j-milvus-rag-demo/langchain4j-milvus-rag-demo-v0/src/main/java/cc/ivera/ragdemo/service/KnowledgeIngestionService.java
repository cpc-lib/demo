package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.service.ingest.Splitter;
import cc.ivera.ragdemo.service.ingest.TikaParser;
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

    // ✅ 关键：embedding 接口一次最多 10 条 input.contents
    private static final int EMBED_BATCH_SIZE = 10;

    private final TikaParser tikaParser;
    private final Splitter splitter;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 强兼容：把 metadata 转成 Map
     * - 有些版本 metadata() 返回 Metadata，支持 toMap()
     * - 有些版本 metadata() 可能返回 Map
     * - 有些版本可能为 null
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeToMap(Object metadataObj) {
        if (metadataObj == null) {
            return Collections.emptyMap();
        }

        // 情况1：已经是 Map
        if (metadataObj instanceof Map<?, ?> m) {
            Map<String, Object> out = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }

        // 情况2：是 Metadata 且有 toMap()
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
        } catch (Exception ignore) {}

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

        // 1) 合并 Document 级元数据（文件名/source 等）
        Map<String, Object> merged = new HashMap<>(safeToMap(document.metadata()));
        merged.putAll(extraMetadata);

        Document docWithMeta = Document.from(document.text(), Metadata.from(merged));

        // 2) 切片（沿用你项目里的 Splitter，保持 chunk-size/overlap 一致）
        List<TextSegment> segments = splitter.split(docWithMeta);

        // 3) 给每个分片补充 metadata + chunkId（可选但推荐）
        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);

            Map<String, Object> segMeta = new HashMap<>(safeToMap(seg.metadata()));
            segMeta.putAll(extraMetadata);
            segMeta.put("chunkId", i);

            segments.set(i, TextSegment.from(seg.text(), Metadata.from(segMeta)));
        }

        // 4) ✅ 分批 embedding（<=10）+ 写入 Milvus，彻底规避 batch size > 10
        for (int from = 0; from < segments.size(); from += EMBED_BATCH_SIZE) {
            int to = Math.min(from + EMBED_BATCH_SIZE, segments.size());

            List<TextSegment> batchSegs = new ArrayList<>(segments.subList(from, to));

            Response<List<Embedding>> resp = embeddingModel.embedAll(batchSegs);
            List<Embedding> embeddings = resp.content();

            // 写入向量库：embeddings 与 batchSegs 一一对应
            embeddingStore.addAll(embeddings, batchSegs);
        }

        return segments.size();
    }
}
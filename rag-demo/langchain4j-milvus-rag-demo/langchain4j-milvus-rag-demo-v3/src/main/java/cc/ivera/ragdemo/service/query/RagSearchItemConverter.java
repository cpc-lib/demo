package cc.ivera.ragdemo.service.query;

import cc.ivera.ragdemo.model.query.RagSearchItem;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RagSearchItemConverter {

    @Autowired
    public RagSearchItemConverter() {
    }

    public List<RagSearchItem> fromMatches(List<EmbeddingMatch<TextSegment>> matches) {
        List<RagSearchItem> items = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            TextSegment segment = match.embedded();
            Map<String, Object> metadata = metadataOf(segment);
            items.add(new RagSearchItem(
                    i + 1,
                    match.score(),
                    longValue(metadata.get("knowledge_base_id")),
                    stringValue(metadata.get("document_id")),
                    stringValue(metadata.get("fileName")),
                    firstNonBlank(stringValue(metadata.get("chunk_id")), stringValue(metadata.get("chunkId"))),
                    integerValue(metadata.get("version")),
                    stringValue(metadata.get("content_type")),
                    integerValue(metadata.get("page_no")),
                    stringValue(metadata.get("section_title")),
                    stringValue(metadata.get("image_caption")),
                    stringValue(metadata.get("image_number")),
                    stringValue(metadata.get("image_url")),
                    segment == null ? "" : segment.text(),
                    metadata,
                    firstNonBlank(stringValue(metadata.get("modality")), modalityFromContentType(stringValue(metadata.get("content_type")))),
                    firstNonBlank(stringValue(metadata.get("retrieval_source")), "text_vector"),
                    longValue(metadata.get("image_asset_id")),
                    doubleValue(metadata.get("fusion_score"))
            ));
        }
        return items;
    }

    public List<RagSearchItem> rerank(List<RagSearchItem> items) {
        List<RagSearchItem> ranked = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            RagSearchItem item = items.get(i);
            ranked.add(new RagSearchItem(
                    i + 1,
                    item.score(),
                    item.knowledgeBaseId(),
                    item.documentId(),
                    item.documentName(),
                    item.chunkId(),
                    item.version(),
                    item.contentType(),
                    item.pageNo(),
                    item.sectionTitle(),
                    item.imageCaption(),
                    item.imageNumber(),
                    item.imageUrl(),
                    item.content(),
                    item.metadata(),
                    item.modality(),
                    item.retrievalSource(),
                    item.imageAssetId(),
                    item.fusionScore()
            ));
        }
        return ranked;
    }

    private Map<String, Object> metadataOf(TextSegment segment) {
        if (segment == null || segment.metadata() == null) {
            return Map.of();
        }
        Metadata metadata = segment.metadata();
        return new LinkedHashMap<>(metadata.toMap());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double doubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String modalityFromContentType(String contentType) {
        if (contentType == null) {
            return "text";
        }
        String normalized = contentType.toLowerCase();
        return normalized.contains("image") || normalized.contains("chart") || normalized.contains("table")
                ? "image"
                : "text";
    }
}

package cc.ivera.ragdemo.service.tool;


import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.SourceType;
import cc.ivera.ragdemo.service.rag.Retriever;
import cc.ivera.ragdemo.service.trace.AgentTraceContext;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class KnowledgeTool {

    private final Retriever retriever;

    @Tool("Search the private enterprise knowledge base. Use this first for uploaded documents, project docs, code, business rules, internal knowledge, images, charts, flowcharts and architecture diagrams.")
    public String knowledgeSearch(String query) {
        List<Content> contents = retriever.retrieve(query);
        if (contents == null || contents.isEmpty()) {
            AgentTraceContext.current().addToolTrace("knowledgeSearch", "knowledge base not hit");
            return "KNOWLEDGE_BASE_NOT_FOUND";
        }

        List<SourceItem> sourceItems = new ArrayList<>();
        String joined = contents.stream()
                .filter(Objects::nonNull)
                .limit(8)
                .map(content -> {
                    String text = contentToText(content);
                    Map<String, Object> metadata = metadataOf(content);
                    String fileName = stringValue(metadata.get("fileName"));
                    String chunkId = stringValue(metadata.get("chunk_id"));
                    if (chunkId == null) {
                        chunkId = stringValue(metadata.get("chunkId"));
                    }
                    String contentType = stringValue(metadata.get("content_type"));
                    Integer pageNo = integerValue(metadata.get("page_no"));

                    sourceItems.add(SourceItem.builder()
                            .type(SourceType.KNOWLEDGE_BASE)
                            .title(fileName != null ? fileName : stringValue(metadata.get("source")))
                            .fileName(fileName)
                            .chunkId(chunkId)
                            .version(integerValue(metadata.get("version")))
                            .chunkStatus(stringValue(metadata.get("chunk_status")))
                            .contentType(contentType)
                            .imageUrl(stringValue(metadata.get("image_url")))
                            .pageNo(pageNo)
                            .sectionTitle(stringValue(metadata.get("section_title")))
                            .imageCaption(stringValue(metadata.get("image_caption")))
                            .imageNumber(stringValue(metadata.get("image_number")))
                            .content(text)
                            .build());

                    return "[knowledge_chunk]"
                            + field("file", fileName)
                            + field("type", contentType)
                            + field("page", pageNo)
                            + field("section", metadata.get("section_title"))
                            + field("caption", metadata.get("image_caption"))
                            + field("imageNo", metadata.get("image_number"))
                            + field("image", metadata.get("image_url"))
                            + field("chunk", chunkId)
                            + field("version", metadata.get("version"))
                            + "\n" + text;
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        AgentTraceContext.current().setKnowledgeHit(true);
        AgentTraceContext.current().addSources(sourceItems);
        AgentTraceContext.current().addToolTrace("knowledgeSearch", "knowledge chunks hit=" + sourceItems.size());
        return joined;
    }

    private String field(String name, Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return "";
        }
        return " " + name + "=" + value;
    }

    private String contentToText(Content c) {
        if (c == null) {
            return "";
        }
        try {
            Method text = c.getClass().getMethod("text");
            Object value = text.invoke(c);
            if (value != null) {
                return value.toString();
            }
        } catch (Exception ignore) {
        }

        try {
            Method textSegment = c.getClass().getMethod("textSegment");
            Object segment = textSegment.invoke(c);
            if (segment != null) {
                Method text = segment.getClass().getMethod("text");
                Object value = text.invoke(segment);
                if (value != null) {
                    return value.toString();
                }
            }
        } catch (Exception ignore) {
        }
        return c.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> metadataOf(Content c) {
        try {
            Method textSegment = c.getClass().getMethod("textSegment");
            Object segment = textSegment.invoke(c);
            if (segment == null) {
                return Map.of();
            }
            Method metadataMethod = segment.getClass().getMethod("metadata");
            Object metadata = metadataMethod.invoke(segment);
            if (metadata == null) {
                return Map.of();
            }
            if (metadata instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            Method toMap = metadata.getClass().getMethod("toMap");
            Object value = toMap.invoke(metadata);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (Exception ignore) {
        }
        return Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignore) {
            return null;
        }
    }
}

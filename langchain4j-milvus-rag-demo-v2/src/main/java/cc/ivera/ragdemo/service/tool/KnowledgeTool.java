package cc.ivera.ragdemo.service.tool;

import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.SourceType;
import cc.ivera.ragdemo.service.rag.Retriever;
import cc.ivera.ragdemo.service.trace.AgentTraceContext;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KnowledgeTool {

    private final Retriever retriever;

    @Tool("查询企业私有知识库。适用于项目文档、代码实现、业务规则、内部知识、上传文件问答。必须优先使用；若未命中或信息不足，再考虑互联网搜索。")
    public String knowledgeSearch(String query) {
        List<Content> contents = retriever.retrieve(query);
        if (contents == null || contents.isEmpty()) {
            AgentTraceContext.current().addToolTrace("knowledgeSearch", "未命中知识库");
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
                    Integer chunkId = integerValue(metadata.get("chunkId"));
                    sourceItems.add(SourceItem.builder()
                            .type(SourceType.KNOWLEDGE_BASE)
                            .title(fileName != null ? fileName : stringValue(metadata.get("source")))
                            .fileName(fileName)
                            .chunkId(chunkId)
                            .content(text)
                            .build());
                    return "[知识片段]"
                            + (fileName != null ? (" 文件=" + fileName) : "")
                            + (chunkId != null ? (" chunk=" + chunkId) : "")
                            + "\n" + text;
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        AgentTraceContext.current().setKnowledgeHit(true);
        AgentTraceContext.current().addSources(sourceItems);
        AgentTraceContext.current().addToolTrace("knowledgeSearch", "命中知识库片段数=" + sourceItems.size());
        return joined;
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

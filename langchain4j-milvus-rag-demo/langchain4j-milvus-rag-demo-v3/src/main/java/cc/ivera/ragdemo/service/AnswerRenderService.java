package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.ChatAnswer;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.ToolTrace;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class AnswerRenderService {

    private final RagProperties properties;

    public String render(ChatAnswer answer) {
        StringBuilder sb = new StringBuilder();
        sb.append(answer.answer() == null ? "" : answer.answer().trim());

        if (properties.getAgent().isAppendSourceBlock() && answer.sources() != null && !answer.sources().isEmpty()) {
            sb.append("\n\n[Sources]\n");
            int index = 1;
            for (SourceItem source : answer.sources()) {
                sb.append(index++)
                        .append(". ")
                        .append(formatSource(source))
                        .append("\n");
            }
        }

        if (properties.getAgent().isAppendToolTrace() && answer.toolTraces() != null && !answer.toolTraces().isEmpty()) {
            sb.append("\n[Tool Trace]\n");
            sb.append(answer.toolTraces().stream()
                    .map(this::formatToolTrace)
                    .collect(Collectors.joining("\n")));
        }
        return sb.toString().trim();
    }

    private String formatSource(SourceItem source) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(source.type()).append("] ");
        if (source.title() != null && !source.title().isBlank()) {
            sb.append(source.title());
        } else if (source.fileName() != null && !source.fileName().isBlank()) {
            sb.append(source.fileName());
        } else {
            sb.append("untitled source");
        }
        append(sb, "file", source.fileName());
        append(sb, "chunk", source.chunkId());
        append(sb, "version", source.version());
        append(sb, "status", source.chunkStatus());
        append(sb, "type", source.contentType());
        append(sb, "page", source.pageNo());
        append(sb, "section", source.sectionTitle());
        append(sb, "imageNo", source.imageNumber());
        append(sb, "caption", truncate(source.imageCaption(), 80));
        append(sb, "image", source.imageUrl());
        append(sb, "url", source.url());

        String content = source.content();
        if (content != null && !content.isBlank()) {
            append(sb, "summary", truncate(content, 140));
        }
        return sb.toString();
    }

    private void append(StringBuilder sb, String name, Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return;
        }
        sb.append(" | ").append(name).append("=").append(value);
    }

    private String formatToolTrace(ToolTrace trace) {
        return "- " + trace.toolName() + ": " + trace.summary();
    }

    private String truncate(String text, int limit) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "...";
    }

    public List<SourceItem> deduplicate(List<SourceItem> sourceItems) {
        if (sourceItems == null) {
            return List.of();
        }
        return sourceItems.stream()
                .distinct()
                .limit(10)
                .toList();
    }
}

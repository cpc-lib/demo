package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.ChatAnswer;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.ToolTrace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerRenderService {

    private final RagProperties properties;

    public String render(ChatAnswer answer) {
        StringBuilder sb = new StringBuilder();
        sb.append(answer.answer() == null ? "" : answer.answer().trim());

        if (properties.getAgent().isAppendSourceBlock() && answer.sources() != null && !answer.sources().isEmpty()) {
            sb.append("\n\n【数据来源】\n");
            int index = 1;
            for (SourceItem source : answer.sources()) {
                sb.append(index++)
                        .append(". ")
                        .append(formatSource(source))
                        .append("\n");
            }
        }

        if (properties.getAgent().isAppendToolTrace() && answer.toolTraces() != null && !answer.toolTraces().isEmpty()) {
            sb.append("\n【工具调用轨迹】\n");
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
            sb.append("未命名来源");
        }
        if (source.fileName() != null && !source.fileName().isBlank() && (source.title() == null || !source.title().equals(source.fileName()))) {
            sb.append(" | file=").append(source.fileName());
        }
        if (source.chunkId() != null) {
            sb.append(" | chunk=").append(source.chunkId());
        }
        if (source.url() != null && !source.url().isBlank()) {
            sb.append(" | ").append(source.url());
        }
        String content = source.content();
        if (content != null && !content.isBlank()) {
            sb.append(" | 摘要=").append(truncate(content, 140));
        }
        return sb.toString();
    }

    private String formatToolTrace(ToolTrace trace) {
        return "- " + trace.toolName() + "：" + trace.summary();
    }

    private String truncate(String text, int limit) {
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

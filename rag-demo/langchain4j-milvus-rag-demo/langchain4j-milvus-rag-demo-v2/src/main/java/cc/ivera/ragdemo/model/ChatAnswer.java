package cc.ivera.ragdemo.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatAnswer(
        String conversationId,
        String question,
        String answer,
        boolean knowledgeHit,
        boolean webSearchUsed,
        boolean weatherUsed,
        List<SourceItem> sources,
        List<ToolTrace> toolTraces
) {
}

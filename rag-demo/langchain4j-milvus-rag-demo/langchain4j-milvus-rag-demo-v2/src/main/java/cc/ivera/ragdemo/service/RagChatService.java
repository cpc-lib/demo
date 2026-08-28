package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.agent.AgentAssistant;
import cc.ivera.ragdemo.model.ChatAnswer;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.service.trace.AgentTraceContext;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class RagChatService {

    private final AgentAssistant agentAssistant;
    private final AnswerRenderService answerRenderService;

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    public SseEmitter streamAnswer(String conversationId, String question) {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onTimeout(emitter::complete);

        sseExecutor.execute(() -> {
            try {
                ChatAnswer answer = answerDetailed(conversationId, question);
                String rendered = answerRenderService.render(answer);
                for (String chunk : chunks(rendered, 80)) {
                    send(emitter, "token", chunk);
                }
                send(emitter, "done", "[DONE]");
                emitter.complete();
            } catch (Exception e) {
                send(emitter, "error", e.getMessage());
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    public String answer(String conversationId, String question) {
        return answerRenderService.render(answerDetailed(conversationId, question));
    }

    public ChatAnswer answerDetailed(String conversationId, String question) {
        String finalConversationId = normalizeConversationId(conversationId);
        AgentTraceContext.init(question);
        try {
            Result<String> result = agentAssistant.chat(finalConversationId, question);
            AgentTraceContext.TraceState traceState = AgentTraceContext.get();
            List<SourceItem> sources = answerRenderService.deduplicate(traceState.getSources());
            return ChatAnswer.builder()
                    .conversationId(finalConversationId)
                    .question(question)
                    .answer(result == null ? "" : result.content())
                    .knowledgeHit(traceState.isKnowledgeHit())
                    .webSearchUsed(traceState.isWebSearchUsed())
                    .weatherUsed(traceState.isWeatherUsed())
                    .sources(sources)
                    .toolTraces(List.copyOf(traceState.getToolTraces()))
                    .build();
        } finally {
            AgentTraceContext.clear();
        }
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "conv-" + UUID.randomUUID();
        }
        return conversationId.trim();
    }

    private List<String> chunks(String text, int chunkSize) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }
        int size = Math.max(1, chunkSize);
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < text.length(); i += size) {
            out.add(text.substring(i, Math.min(i + size, text.length())));
        }
        return out;
    }

    private void send(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException ignored) {
            emitter.complete();
        }
    }
}

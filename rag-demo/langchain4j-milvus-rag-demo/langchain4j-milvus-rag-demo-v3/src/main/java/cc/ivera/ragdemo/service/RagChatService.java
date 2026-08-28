package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.agent.AgentAssistant;
import cc.ivera.ragdemo.model.ChatAnswer;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.service.trace.AgentTraceContext;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagChatService {

    private final AgentAssistant agentAssistant;
    private final AnswerRenderService answerRenderService;

    /**
     * SSE线程池配置：
     * - 核心线程数：4（CPU核心数的一半）
     * - 最大线程数：CPU核心数 * 2，最多32
     * - 任务队列：容量1000，防止内存溢出
     * - 空闲线程存活时间：60秒
     * - 拒绝策略：CallerRunsPolicy，让调用者线程执行任务
     */
    private final ExecutorService sseExecutor = new ThreadPoolExecutor(
            4,                                      // corePoolSize: 核心线程数
            Math.min(Runtime.getRuntime().availableProcessors() * 2, 32),  // maxPoolSize: 最大线程数
            60L,                                    // keepAliveTime: 空闲线程存活时间
            TimeUnit.SECONDS,                       // 时间单位
            new LinkedBlockingQueue<>(1000),        // workQueue: 任务队列，容量1000
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("sse-worker-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：调用者执行
    );

    /**
     * SSE连接超时时间：5分钟（300秒）
     */
    private static final long SSE_TIMEOUT_MS = 300_000L;

    public SseEmitter streamAnswer(String conversationId, String question) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
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
        } catch (IOException | IllegalStateException e) {
            log.debug("Failed to send SSE event, emitter already closed: {}", e.getMessage());
            emitter.complete();
        }
    }
}

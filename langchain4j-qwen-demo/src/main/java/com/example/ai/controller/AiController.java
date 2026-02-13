package com.example.ai.controller;

import com.example.ai.controller.dto.ChatRequest;
import com.example.ai.controller.dto.ImageReq;
import com.example.ai.controller.dto.ImageResp;
import com.example.ai.image.QwenImageClient;
import com.example.ai.service.Assistant;
import com.example.ai.service.StreamAssistant;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AiController {

    private final Assistant assistant;
    private final StreamAssistant streamAssistant;

    private final QwenImageClient qwenImageClient;

    public AiController(Assistant assistant, StreamAssistant streamAssistant, QwenImageClient qwenImageClient
    ) {
        this.assistant = assistant;
        this.streamAssistant = streamAssistant;
        this.qwenImageClient = qwenImageClient;

    }

    @PostMapping("/chat")
    public Mono<String> chat(@RequestBody ChatRequest req) {
        String sessionId = req.sessionId() == null ? "default" : req.sessionId();
        String msg = req.message();

        return Mono.fromCallable(() -> assistant.chat(sessionId, msg))
                .subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * Agent-level SSE (LangChain4j TokenStream):
     * - event: token        data: incremental token
     * - event: retrieved    data: brief retrieved chunks (RAG)
     * - event: tool_start   data: {"name":"...","arguments":...}
     * - event: tool_end     data: tool result (string)
     * - event: done         data: [DONE]
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody ChatRequest req) {

        String sessionId = req.sessionId() == null ? "default" : req.sessionId();
        String message = req.message();

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();

        // ✅ 关键：把 Agent 执行放到 boundedElastic
        Schedulers.boundedElastic().schedule(() -> {
            try {
                TokenStream ts = streamAssistant.chat(sessionId, message);

                ts.onPartialResponse(token ->
                                sink.tryEmitNext(ServerSentEvent.builder(token).event("token").build()))
                        .onRetrieved((List<Content> contents) -> {
                            String joined = contents.stream()
                                    .map(AiController::contentToText) // 你之前修复过的
                                    .map(s -> safeSnippet(s, 220))
                                    .limit(5)
                                    .collect(Collectors.joining("\n---\n"));
                            sink.tryEmitNext(ServerSentEvent.builder(joined).event("retrieved").build());
                        })
                        .beforeToolExecution(bte -> {
                            var r = bte.request();
                            String args = toJsonLike(r.arguments());
                            String json = String.format("{\"name\":\"%s\",\"arguments\":%s}", escape(r.name()), args);
                            sink.tryEmitNext(ServerSentEvent.builder(json).event("tool_start").build());
                        })
                        .onToolExecuted(te -> {
                            String out = String.valueOf(te.result());
                            sink.tryEmitNext(ServerSentEvent.builder(out).event("tool_end").build());
                        })
                        .onCompleteResponse(resp -> {
                            sink.tryEmitNext(ServerSentEvent.builder("[DONE]").event("done").build());
                            sink.tryEmitComplete();
                        })
                        .onError(err -> sink.tryEmitError(err))
                        .start();

            } catch (Throwable e) {
                sink.tryEmitError(e);
            }
        });

        return sink.asFlux().timeout(Duration.ofMinutes(3));
    }


    @PostMapping("/image")
    public ImageResp generate(@RequestBody ImageReq req) {
        String url = qwenImageClient.generateImageUrl(req.prompt(), req.size(), req.negativePrompt());
        return new ImageResp(url, "URL 通常是临时有效，请尽快下载保存");
    }


    /**
     * ✅ 兼容不同版本的 Content：
     * - 尝试 text()
     * - 尝试 getText()
     * - 都没有就用 toString()
     */
    private static String contentToText(Content c) {
        if (c == null) return "";

        // 1) try text()
        try {
            var m = c.getClass().getMethod("text");
            Object v = m.invoke(c);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) {
        }

        // 2) try getText()
        try {
            var m = c.getClass().getMethod("getText");
            Object v = m.invoke(c);
            if (v != null) return String.valueOf(v);
        } catch (Exception ignored) {
        }

        // 3) fallback
        return String.valueOf(c);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private static String safeSnippet(String s, int max) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }

    private static String toJsonLike(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String s) {
            return "\"" + escape(s) + "\"";
        }
        // 为稳妥起见统一包成 JSON 字符串，避免拼出非法 JSON
        return "\"" + escape(String.valueOf(obj)) + "\"";
    }
}

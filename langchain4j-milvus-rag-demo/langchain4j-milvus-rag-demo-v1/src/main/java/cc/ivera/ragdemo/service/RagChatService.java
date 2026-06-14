package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.service.rag.PromptBuilder;
import cc.ivera.ragdemo.service.rag.Retriever;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.content.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatService {

    private final Retriever retriever;
    private final PromptBuilder promptBuilder;
    private final StreamingChatLanguageModel streamingChatModel;
    private final ChatLanguageModel chatModel;
    private final WebSearchService webSearchService;

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    private static Method findStreamingMethod(Class<?> clazz, String methodName, Class<?> handlerType) {
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 2 && ps[0] == String.class && ps[1].isAssignableFrom(handlerType)) {
                return m;
            }
            if (ps.length == 2 && ps[0] == String.class && handlerType.isAssignableFrom(ps[1])) {
                return m;
            }
        }
        return null;
    }

    private static Class<?> tryLoad(String... classNames) {
        for (String cn : classNames) {
            try {
                return Class.forName(cn);
            } catch (ClassNotFoundException ignore) {
            }
        }
        return null;
    }

//    public SseEmitter streamAnswer(String question) {
//        SseEmitter emitter = new SseEmitter(0L);
//
//        // 客户端关闭/超时，尽快结束
//        emitter.onTimeout(emitter::complete);
//        emitter.onCompletion(() -> {
//        });
//
//        sseExecutor.execute(() -> {
//            try {
//                List<Content> contents = retriever.retrieve(question);
//
//                List<String> chunks = contents.stream()
//                        .map(this::contentToText)
//                        .collect(Collectors.toList());
//
//                String prompt = promptBuilder.build(question, chunks);
//
//                streamGenerate(prompt, emitter);
//
//            } catch (Exception e) {
//                send(emitter, "error", e.getMessage());
//                emitter.completeWithError(e);
//            }
//        });
//
//        return emitter;
//    }

    public SseEmitter streamAnswer(String question) {
        SseEmitter emitter = new SseEmitter(0L);

        // 客户端关闭/超时，尽快结束
        emitter.onTimeout(emitter::complete);
        emitter.onCompletion(() -> {
        });

        sseExecutor.execute(() -> {
            try {
                List<Content> contents = retriever.retrieve(question);

                List<String> chunks = contents.stream()
                        .map(this::contentToText)
                        .collect(Collectors.toList());

                String prompt;

                // 1. 知识库命中
                if (chunks != null && !chunks.isEmpty()) {

                    log.info("RAG knowledge hit, size={}", chunks.size());
                    prompt = promptBuilder.build(question, chunks);

                } else {
                    log.info("RAG miss, using web search...");
                    // 2. 未命中 → 联网搜索
                    String webResult = webSearchService.search(question);

                    prompt = """
                            用户问题：
                            %s
                            
                            以下是互联网搜索结果：
                            
                            %s
                            
                            请根据搜索结果回答用户问题。
                            """.formatted(question, webResult);
                }

                streamGenerate(prompt, emitter);

            } catch (Exception e) {
                send(emitter, "error", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }


//    public String answer(String question) {
//        List<Content> contents = retriever.retrieve(question);
//
//        List<String> chunks = contents.stream()
//                .map(this::contentToText)
//                .collect(Collectors.toList());
//
//        String prompt = promptBuilder.build(question, chunks);
//
//        // ✅ 同步阻塞返回
//        return chatModel.generate(prompt);
//    }


    public String answer(String question) {

        List<Content> contents = retriever.retrieve(question);

        List<String> chunks = contents.stream()
                .map(this::contentToText)
                .collect(Collectors.toList());

        String prompt;

        // 1. 知识库命中
        if (chunks != null && !chunks.isEmpty()) {

            log.info("RAG knowledge hit, size={}", chunks.size());
            prompt = promptBuilder.build(question, chunks);

        } else {
            log.info("RAG miss, using web search...");
            // 2. 未命中 → AI联网搜索
            String webResult = webSearchService.search(question);

            prompt = """
                    用户问题：
                    %s
                    
                    以下是互联网搜索结果：
                    
                    %s
                    
                    请根据搜索结果回答用户问题。
                    """.formatted(question, webResult);
        }

        return chatModel.generate(prompt);
    }

    /**
     * 强兼容：自动适配不同版本 LangChain4j 的 handler 方法名与模型方法名：
     * handler: onNext/onComplete/onError 或 onPartialResponse/onCompleteResponse/onError
     * model: generate(prompt, handler) 或 chat(prompt, handler)
     */
    private void streamGenerate(String prompt, SseEmitter emitter) throws Exception {

        // 1) 找到 handler 接口（不同版本可能类名不同）
        Class<?> handlerType = tryLoad(
                "dev.langchain4j.model.chat.response.StreamingChatResponseHandler",
                "dev.langchain4j.model.StreamingResponseHandler"
        );
        if (handlerType == null) {
            throw new IllegalStateException("Cannot find streaming handler interface in classpath.");
        }

        // 2) 动态实现 handler：同时兼容两套回调方法名
        Object handler = Proxy.newProxyInstance(
                handlerType.getClassLoader(),
                new Class<?>[]{handlerType},
                (proxy, method, args) -> {
                    String name = method.getName();

                    // token：onNext(token) 或 onPartialResponse(token)
                    if ("onNext".equals(name) || "onPartialResponse".equals(name)) {
                        if (args != null && args.length > 0 && args[0] != null) {
                            send(emitter, "token", String.valueOf(args[0]));
                        }
                        return null;
                    }

                    // complete：onComplete(resp) 或 onCompleteResponse(resp)
                    if ("onComplete".equals(name) || "onCompleteResponse".equals(name)) {
                        send(emitter, "done", "[DONE]");
                        emitter.complete();
                        return null;
                    }

                    // error：onError(Throwable)
                    if ("onError".equals(name)) {
                        Throwable t = (args != null && args.length > 0 && args[0] instanceof Throwable)
                                ? (Throwable) args[0]
                                : new RuntimeException("Unknown streaming error");
                        send(emitter, "error", t.getMessage());
                        emitter.completeWithError(t);
                        return null;
                    }

                    return null;
                }
        );

        // 3) 调用 streamingChatModel 的 streaming 方法：优先 generate，其次 chat
        Method m = findStreamingMethod(streamingChatModel.getClass(), "generate", handlerType);
        if (m == null) {
            m = findStreamingMethod(streamingChatModel.getClass(), "chat", handlerType);
        }
        if (m == null) {
            throw new IllegalStateException("Cannot find streaming method generate/chat (String, handler) on: "
                    + streamingChatModel.getClass().getName());
        }

        m.invoke(streamingChatModel, prompt, handler);
    }

    private void send(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException ignored) {
            // 客户端断开/连接被关闭时，会抛 IllegalStateException
            emitter.complete();
        }
    }


    private String contentToText(Content c) {
        if (c == null) return "";

        // 兼容：Content.text()
        try {
            var m = c.getClass().getMethod("text");
            Object v = m.invoke(c);
            if (v != null) return v.toString();
        } catch (Exception ignore) {
        }

        // 兼容：Content.textSegment().text()
        try {
            var m = c.getClass().getMethod("textSegment");
            Object seg = m.invoke(c);
            if (seg != null) {
                var mt = seg.getClass().getMethod("text");
                Object v = mt.invoke(seg);
                if (v != null) return v.toString();
            }
        } catch (Exception ignore) {
        }

        // 兜底
        return c.toString();
    }
}
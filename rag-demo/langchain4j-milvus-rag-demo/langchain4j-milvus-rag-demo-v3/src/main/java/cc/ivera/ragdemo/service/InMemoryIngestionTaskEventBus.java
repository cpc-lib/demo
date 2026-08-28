package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagIngestionTaskEvent;
import cc.ivera.ragdemo.model.knowledge.IngestionTaskEventView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class InMemoryIngestionTaskEventBus implements IngestionTaskEventBus {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIngestionTaskEventBus.class);
    private static final Set<String> TERMINAL_EVENTS = Set.of(
            "TASK_SUCCEEDED",
            "TASK_PARTIAL_SUCCESS",
            "TASK_FAILED",
            "TASK_CANCELLED"
    );

    private final RagProperties properties;
    private final Supplier<SseEmitter> emitterFactory;
    private final Map<SubscriptionKey, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public InMemoryIngestionTaskEventBus(RagProperties properties) {
        this.properties = properties;
        this.emitterFactory = () -> new SseEmitter(properties.getIngestionEvents().getEmitterTimeoutMillis());
    }

    InMemoryIngestionTaskEventBus(RagProperties properties, Supplier<SseEmitter> emitterFactory) {
        this.properties = properties;
        this.emitterFactory = emitterFactory;
    }

    @Override
    public SseEmitter subscribe(Long tenantId, Long taskId, String lastEventId) {
        SseEmitter emitter = emitterFactory.get();
        SubscriptionKey key = key(tenantId, taskId);
        subscribers.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(ignored -> remove(key, emitter));
        return emitter;
    }

    @Override
    public void publish(Long tenantId, RagIngestionTaskEvent event) {
        if (event == null || event.getTaskId() == null) {
            return;
        }
        SubscriptionKey key = key(tenantId, event.getTaskId());
        List<SseEmitter> emitters = subscribers.get(key);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        IngestionTaskEventView view = IngestionTaskEventPublisher.toView(event);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.getId() == null ? "" : String.valueOf(event.getId()))
                        .name(event.getEventType())
                        .data(view));
                if (TERMINAL_EVENTS.contains(event.getEventType())) {
                    emitter.complete();
                    remove(key, emitter);
                }
            } catch (IOException | IllegalStateException e) {
                log.debug("Remove failed ingestion task SSE subscriber for task {}: {}", event.getTaskId(), e.getMessage());
                remove(key, emitter);
            }
        }
    }

    private void remove(SubscriptionKey key, SseEmitter emitter) {
        List<SseEmitter> emitters = subscribers.get(key);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            subscribers.remove(key);
        }
    }

    private SubscriptionKey key(Long tenantId, Long taskId) {
        return new SubscriptionKey(tenantId == null ? 0L : tenantId, taskId);
    }

    private record SubscriptionKey(Long tenantId, Long taskId) {
    }
}

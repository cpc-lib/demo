package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.rag.RagIngestionTaskEvent;
import cc.ivera.ragdemo.model.knowledge.IngestionTaskEventView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class RedisStreamIngestionTaskEventBus implements IngestionTaskEventBus {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamIngestionTaskEventBus.class);
    private static final Set<String> TERMINAL_EVENTS = Set.of(
            "TASK_SUCCEEDED",
            "TASK_PARTIAL_SUCCESS",
            "TASK_FAILED",
            "TASK_CANCELLED"
    );

    private final RagProperties properties;
    private final StringRedisTemplate redisTemplate;

    public RedisStreamIngestionTaskEventBus(RagProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SseEmitter subscribe(Long tenantId, Long taskId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(properties.getIngestionEvents().getEmitterTimeoutMillis());
        AtomicBoolean open = new AtomicBoolean(true);
        emitter.onCompletion(() -> open.set(false));
        emitter.onTimeout(() -> open.set(false));
        emitter.onError(ignored -> open.set(false));
        CompletableFuture.runAsync(() -> streamLoop(tenantId, taskId, lastEventId, emitter, open));
        return emitter;
    }

    @Override
    public void publish(Long tenantId, RagIngestionTaskEvent event) {
        if (event == null || event.getTaskId() == null) {
            return;
        }
        try {
            String key = streamKey(tenantId, event.getTaskId());
            Map<String, String> payload = payload(event);
            RecordId id = redisTemplate.opsForStream().add(StreamRecords.mapBacked(payload).withStreamKey(key));
            long maxLen = properties.getIngestionEvents().getRedisStreamMaxLen();
            redisTemplate.opsForStream().trim(key, maxLen, true);
            redisTemplate.expire(key, Duration.ofSeconds(properties.getIngestionEvents().getStreamTtlSeconds()));
            log.debug("Published ingestion task event to Redis Stream: key={}, id={}", key, id);
        } catch (Exception ex) {
            log.warn("Failed to publish ingestion task event to Redis Stream for task {}: {}",
                    event.getTaskId(), ex.getMessage());
        }
    }

    private void streamLoop(Long tenantId,
                            Long taskId,
                            String lastEventId,
                            SseEmitter emitter,
                            AtomicBoolean open) {
        String key = streamKey(tenantId, taskId);
        ReadOffset offset = StringUtils.hasText(lastEventId) ? ReadOffset.from(lastEventId.trim()) : ReadOffset.latest();
        Duration block = Duration.ofMillis(Math.max(100, properties.getIngestionEvents().getPollIntervalMillis()));
        while (open.get()) {
            try {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                        StreamReadOptions.empty().count(100).block(block),
                        StreamOffset.create(key, offset)
                );
                if (records == null || records.isEmpty()) {
                    sendHeartbeat(emitter, taskId);
                    continue;
                }
                for (MapRecord<String, Object, Object> record : records) {
                    offset = ReadOffset.from(record.getId().getValue());
                    IngestionTaskEventView view = view(record.getValue());
                    emitter.send(SseEmitter.event()
                            .id(record.getId().getValue())
                            .name(view.eventType())
                            .data(view));
                    if (TERMINAL_EVENTS.contains(view.eventType())) {
                        open.set(false);
                        emitter.complete();
                        return;
                    }
                }
            } catch (IOException | IllegalStateException ex) {
                open.set(false);
                log.debug("Close Redis Stream SSE for task {}: {}", taskId, ex.getMessage());
                emitter.completeWithError(ex);
            } catch (Exception ex) {
                open.set(false);
                log.warn("Redis Stream SSE failed for task {}: {}", taskId, ex.getMessage());
                emitter.completeWithError(ex);
            }
        }
    }

    private void sendHeartbeat(SseEmitter emitter, Long taskId) throws IOException {
        emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data(Map.of("taskId", taskId, "type", "heartbeat")));
    }

    private Map<String, String> payload(RagIngestionTaskEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        put(fields, "id", event.getId());
        put(fields, "taskId", event.getTaskId());
        put(fields, "eventType", event.getEventType());
        put(fields, "stageCode", event.getStageCode());
        put(fields, "shardKey", event.getShardKey());
        put(fields, "progress", event.getProgress());
        put(fields, "stageProgress", event.getStageProgress());
        put(fields, "message", event.getMessage());
        put(fields, "payloadJson", event.getPayloadJson());
        put(fields, "createdAt", event.getCreatedAt());
        return fields;
    }

    private IngestionTaskEventView view(Map<Object, Object> fields) {
        return new IngestionTaskEventView(
                longValue(fields.get("id")),
                longValue(fields.get("taskId")),
                stringValue(fields.get("eventType")),
                stringValue(fields.get("stageCode")),
                stringValue(fields.get("shardKey")),
                intValue(fields.get("progress")),
                intValue(fields.get("stageProgress")),
                stringValue(fields.get("message")),
                stringValue(fields.get("payloadJson")),
                dateTimeValue(fields.get("createdAt"))
        );
    }

    private void put(Map<String, String> fields, String key, Object value) {
        if (value != null) {
            fields.put(key, String.valueOf(value));
        }
    }

    private String streamKey(Long tenantId, Long taskId) {
        return "rag:%s:ingestion-task:%s:events".formatted(tenantId == null ? 0L : tenantId, taskId);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer intValue(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private LocalDateTime dateTimeValue(Object value) {
        return value == null || !StringUtils.hasText(String.valueOf(value))
                ? null
                : LocalDateTime.parse(String.valueOf(value));
    }
}

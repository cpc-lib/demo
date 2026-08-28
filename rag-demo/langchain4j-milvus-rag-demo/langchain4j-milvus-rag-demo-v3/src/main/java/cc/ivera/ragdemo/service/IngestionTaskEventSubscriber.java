package cc.ivera.ragdemo.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Abstraction for subscribing to ingestion task events.
 * <p>
 * The subscriber handles SSE connection lifecycle, Last-Event-ID resume,
 * heartbeat, and tenant permission checks. Implementations may use
 * Redis Stream, in-memory broadcast, or other backends.
 */
public interface IngestionTaskEventSubscriber {

    /**
     * Subscribe to task events via SSE.
     *
     * @param tenantId    the tenant scope for event filtering
     * @param taskId     the ingestion task ID to subscribe to
     * @param lastEventId the last event ID received, for resume (null for fresh subscription)
     * @return the SseEmitter for streaming events to the client
     */
    SseEmitter subscribe(Long tenantId, Long taskId, String lastEventId);
}

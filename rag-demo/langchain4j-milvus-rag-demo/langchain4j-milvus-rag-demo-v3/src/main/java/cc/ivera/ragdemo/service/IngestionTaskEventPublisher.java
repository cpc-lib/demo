package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.domain.rag.RagIngestionTaskEvent;
import cc.ivera.ragdemo.model.knowledge.IngestionTaskEventView;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class IngestionTaskEventPublisher {

    private final IngestionTaskEventBus eventBus;

    public SseEmitter subscribe(Long taskId) {
        return subscribe(TenantContextHolder.currentTenantId().orElse(null), taskId, null);
    }

    public SseEmitter subscribe(Long tenantId, Long taskId, String lastEventId) {
        return eventBus.subscribe(tenantId, taskId, lastEventId);
    }

    public void publish(RagIngestionTaskEvent event) {
        publish(TenantContextHolder.currentTenantId().orElse(null), event);
    }

    public void publish(Long tenantId, RagIngestionTaskEvent event) {
        eventBus.publish(tenantId, event);
    }

    public static IngestionTaskEventView toView(RagIngestionTaskEvent event) {
        return new IngestionTaskEventView(
                event.getId(),
                event.getTaskId(),
                event.getEventType(),
                event.getStageCode(),
                event.getShardKey(),
                event.getProgress(),
                event.getStageProgress(),
                event.getMessage(),
                event.getPayloadJson(),
                event.getCreatedAt()
        );
    }
}

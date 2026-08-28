package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.model.knowledge.RagIngestionTaskMessage;
import cc.ivera.ragdemo.tenant.TenantScopedExecutor;
import cc.ivera.ragdemo.util.TraceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagIngestionTaskConsumer {

    private final RagIngestionExecutor executor;
    private final TenantScopedExecutor tenantScopedExecutor;

    @RabbitListener(queues = "${rag.ingestion.queue-name:rag.ingestion.tasks}",
            autoStartup = "${rag.ingestion.consumer-auto-startup:true}")
    public void consume(RagIngestionTaskMessage message) {
        try {
            if (message.traceId() != null) {
                TraceUtils.setTraceId(message.traceId());
            }
            tenantScopedExecutor.runAsSystemTenant(message.tenantId(), "rabbit-ingestion-consumer",
                    () -> executor.execute(message));
        } finally {
            TraceUtils.clearTraceId();
        }
    }
}

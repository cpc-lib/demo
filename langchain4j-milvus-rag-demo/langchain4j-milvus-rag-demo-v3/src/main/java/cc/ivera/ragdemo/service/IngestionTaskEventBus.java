package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.domain.rag.RagIngestionTaskEvent;

public interface IngestionTaskEventBus extends IngestionTaskEventSubscriber {

    void publish(Long tenantId, RagIngestionTaskEvent event);
}

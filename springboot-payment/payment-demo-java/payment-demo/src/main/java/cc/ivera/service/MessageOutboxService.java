package cc.ivera.service;

import cc.ivera.entity.MessageOutbox;

public interface MessageOutboxService {

    MessageOutbox insertOnce(String eventKey,
                             String aggregateType,
                             String aggregateId,
                             String eventType,
                             String payload);

    void retryFailed(String eventId);
}

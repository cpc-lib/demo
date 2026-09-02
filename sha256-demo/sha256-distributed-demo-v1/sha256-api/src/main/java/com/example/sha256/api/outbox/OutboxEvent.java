package com.example.sha256.api.outbox;

public record OutboxEvent(
        long id,
        String eventId,
        String aggregateId,
        String eventType,
        String broker,
        String payload,
        int retryCount
) {
}

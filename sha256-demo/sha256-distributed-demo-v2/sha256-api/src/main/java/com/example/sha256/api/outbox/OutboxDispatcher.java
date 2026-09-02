package com.example.sha256.api.outbox;

import com.example.sha256.api.broker.TaskPublisher;
import com.example.sha256.api.persistence.TaskPersistenceRepository;
import com.example.sha256.common.model.Sha256TaskMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final TaskPersistenceRepository repository;
    private final TaskPublisher publisher;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final long leaseSeconds;
    private final long baseRetryDelayMillis;
    private final long maxRetryDelayMillis;

    public OutboxDispatcher(TaskPersistenceRepository repository,
                            TaskPublisher publisher,
                            ObjectMapper objectMapper,
                            @Value("${sha256.outbox.batch-size:50}") int batchSize,
                            @Value("${sha256.outbox.lease-seconds:60}") long leaseSeconds,
                            @Value("${sha256.outbox.base-retry-delay-ms:1000}") long baseRetryDelayMillis,
                            @Value("${sha256.outbox.max-retry-delay-ms:60000}") long maxRetryDelayMillis) {
        this.repository = repository;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.leaseSeconds = leaseSeconds;
        this.baseRetryDelayMillis = baseRetryDelayMillis;
        this.maxRetryDelayMillis = maxRetryDelayMillis;
    }

    @Scheduled(fixedDelayString = "${sha256.outbox.poll-interval-ms:1000}")
    public void dispatch() {
        List<OutboxEvent> events = repository.claimBatch(batchSize, leaseSeconds);
        for (OutboxEvent event : events) {
            try {
                Sha256TaskMessage message = objectMapper.readValue(event.payload(), Sha256TaskMessage.class);
                publisher.publish(message).block(Duration.ofSeconds(15));
                repository.markSent(event.id());
            } catch (Exception e) {
                long delay = retryDelay(event.retryCount());
                repository.markRetry(event.id(), event.retryCount(), rootMessage(e), delay);
                log.warn("Outbox publish failed: eventId={}, aggregateId={}, retry={}, nextDelayMs={}",
                        event.eventId(), event.aggregateId(), event.retryCount() + 1, delay, e);
            }
        }
    }

    private long retryDelay(int retryCount) {
        int exponent = Math.min(10, Math.max(0, retryCount));
        long delay;
        try {
            delay = Math.multiplyExact(baseRetryDelayMillis, 1L << exponent);
        } catch (ArithmeticException e) {
            delay = maxRetryDelayMillis;
        }
        return Math.min(maxRetryDelayMillis, delay);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}

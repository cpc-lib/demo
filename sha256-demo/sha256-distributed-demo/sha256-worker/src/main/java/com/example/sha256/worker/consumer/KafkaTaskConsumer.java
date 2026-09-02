package com.example.sha256.worker.consumer;

import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.worker.service.Sha256WorkerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sha256.broker", havingValue = "kafka")
public class KafkaTaskConsumer {
    private final ObjectMapper objectMapper;
    private final Sha256WorkerService workerService;

    public KafkaTaskConsumer(ObjectMapper objectMapper, Sha256WorkerService workerService) {
        this.objectMapper = objectMapper;
        this.workerService = workerService;
    }

    @KafkaListener(
            topics = "${sha256.kafka.topic:sha256.tasks}",
            groupId = "${sha256.kafka.group-id:sha256-workers}",
            concurrency = "${sha256.worker.concurrency:4}"
    )
    public void onMessage(String payload) {
        workerService.process(readMessage(payload));
    }

    private Sha256TaskMessage readMessage(String payload) {
        try {
            return objectMapper.readValue(payload, Sha256TaskMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid Kafka SHA-256 task message", e);
        }
    }
}

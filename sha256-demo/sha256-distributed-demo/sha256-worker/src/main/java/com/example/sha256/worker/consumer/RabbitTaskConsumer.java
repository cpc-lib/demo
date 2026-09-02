package com.example.sha256.worker.consumer;

import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.worker.service.Sha256WorkerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sha256.broker", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitTaskConsumer {
    private final ObjectMapper objectMapper;
    private final Sha256WorkerService workerService;

    public RabbitTaskConsumer(ObjectMapper objectMapper, Sha256WorkerService workerService) {
        this.objectMapper = objectMapper;
        this.workerService = workerService;
    }

    @RabbitListener(queues = "${sha256.rabbit.queue:sha256.tasks}", concurrency = "${sha256.worker.concurrency:4}")
    public void onMessage(String payload) {
        workerService.process(readMessage(payload));
    }

    private Sha256TaskMessage readMessage(String payload) {
        try {
            return objectMapper.readValue(payload, Sha256TaskMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid RabbitMQ SHA-256 task message", e);
        }
    }
}

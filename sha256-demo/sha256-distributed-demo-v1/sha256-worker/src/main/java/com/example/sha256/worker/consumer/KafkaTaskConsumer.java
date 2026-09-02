package com.example.sha256.worker.consumer;

import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.common.repository.RedisTaskRepository;
import com.example.sha256.worker.service.Sha256WorkerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sha256.broker", havingValue = "kafka")
public class KafkaTaskConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaTaskConsumer.class);

    private final ObjectMapper objectMapper;
    private final Sha256WorkerService workerService;
    private final RedisTaskRepository taskRepository;

    public KafkaTaskConsumer(ObjectMapper objectMapper,
                             Sha256WorkerService workerService,
                             RedisTaskRepository taskRepository) {
        this.objectMapper = objectMapper;
        this.workerService = workerService;
        this.taskRepository = taskRepository;
    }

    @RetryableTopic(
            attempts = "${sha256.kafka.retry.attempts:4}",
            backoff = @Backoff(
                    delayExpression = "${sha256.kafka.retry.delay-ms:5000}",
                    multiplierExpression = "${sha256.kafka.retry.multiplier:2.0}",
                    maxDelayExpression = "${sha256.kafka.retry.max-delay-ms:60000}"),
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt",
            autoCreateTopics = "true",
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "${sha256.kafka.topic:sha256.tasks}",
            groupId = "${sha256.kafka.group-id:sha256-workers}",
            concurrency = "${sha256.worker.concurrency:4}"
    )
    public void onMessage(String payload) throws Exception {
        Sha256TaskMessage message = objectMapper.readValue(payload, Sha256TaskMessage.class);
        workerService.process(message);
    }

    @DltHandler
    public void onDlt(String payload) {
        try {
            Sha256TaskMessage message = objectMapper.readValue(payload, Sha256TaskMessage.class);
            taskRepository.markDeadLettered(message.taskId(), "Kafka retry attempts exhausted; message moved to DLT").block();
            log.error("Kafka task moved to DLT: taskId={}", message.taskId());
        } catch (Exception e) {
            log.error("Unable to parse/mark Kafka DLT message", e);
        }
    }
}

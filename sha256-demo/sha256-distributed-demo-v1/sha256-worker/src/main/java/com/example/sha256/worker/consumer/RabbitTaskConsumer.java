package com.example.sha256.worker.consumer;

import com.example.sha256.common.model.Sha256TaskMessage;
import com.example.sha256.common.repository.RedisTaskRepository;
import com.example.sha256.worker.service.Sha256WorkerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "sha256.broker", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitTaskConsumer {
    private static final Logger log = LoggerFactory.getLogger(RabbitTaskConsumer.class);
    private static final String RETRY_HEADER = "x-sha256-retry";

    private final ObjectMapper objectMapper;
    private final Sha256WorkerService workerService;
    private final RedisTaskRepository taskRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String retryExchange;
    private final String retryRoutingKey;
    private final String dlx;
    private final String dlqRoutingKey;
    private final int maxRetries;

    public RabbitTaskConsumer(ObjectMapper objectMapper,
                              Sha256WorkerService workerService,
                              RedisTaskRepository taskRepository,
                              RabbitTemplate rabbitTemplate,
                              @Value("${sha256.rabbit.retry-exchange:sha256.retry.exchange}") String retryExchange,
                              @Value("${sha256.rabbit.retry-routing-key:sha256.retry}") String retryRoutingKey,
                              @Value("${sha256.rabbit.dlx:sha256.dlx}") String dlx,
                              @Value("${sha256.rabbit.dlq-routing-key:sha256.dead}") String dlqRoutingKey,
                              @Value("${sha256.rabbit.max-retries:3}") int maxRetries) {
        this.objectMapper = objectMapper;
        this.workerService = workerService;
        this.taskRepository = taskRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.retryExchange = retryExchange;
        this.retryRoutingKey = retryRoutingKey;
        this.dlx = dlx;
        this.dlqRoutingKey = dlqRoutingKey;
        this.maxRetries = Math.max(0, maxRetries);
    }

    @RabbitListener(queues = "${sha256.rabbit.queue:sha256.tasks}", concurrency = "${sha256.worker.concurrency:4}")
    public void onMessage(Message rawMessage) {
        String payload = new String(rawMessage.getBody(), StandardCharsets.UTF_8);
        Sha256TaskMessage task;
        try {
            task = objectMapper.readValue(payload, Sha256TaskMessage.class);
        } catch (Exception invalid) {
            publishDlq(payload, "invalid-message: " + invalid.getMessage(), 0);
            return;
        }

        try {
            workerService.process(task);
        } catch (Exception processingError) {
            int currentRetry = retryCount(rawMessage);
            if (currentRetry < maxRetries) {
                try {
                    publishRetry(payload, currentRetry + 1, processingError.getMessage());
                    log.warn("RabbitMQ task scheduled for retry: taskId={}, retry={}/{}",
                            task.taskId(), currentRetry + 1, maxRetries);
                } catch (Exception retryPublishError) {
                    taskRepository.markDeadLettered(task.taskId(),
                            "Retry publish failed: " + retryPublishError.getMessage()).block();
                    throw retryPublishError;
                }
            } else {
                taskRepository.markDeadLettered(task.taskId(), processingError.getMessage()).block();
                publishDlq(payload, processingError.getMessage(), currentRetry);
                log.error("RabbitMQ task moved to DLQ: taskId={}, retries={}", task.taskId(), currentRetry);
            }
        }
    }

    private void publishRetry(String payload, int retry, String error) {
        rabbitTemplate.convertAndSend(retryExchange, retryRoutingKey, payload, message -> {
            message.getMessageProperties().setHeader(RETRY_HEADER, retry);
            message.getMessageProperties().setHeader("x-sha256-last-error", truncate(error));
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }

    private void publishDlq(String payload, String error, int retry) {
        rabbitTemplate.convertAndSend(dlx, dlqRoutingKey, payload, message -> {
            message.getMessageProperties().setHeader(RETRY_HEADER, retry);
            message.getMessageProperties().setHeader("x-sha256-last-error", truncate(error));
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }

    private int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(RETRY_HEADER);
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? 0 : Integer.parseInt(value.toString()); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String truncate(String value) {
        if (value == null) return "unknown";
        return value.length() <= 512 ? value : value.substring(0, 512);
    }
}

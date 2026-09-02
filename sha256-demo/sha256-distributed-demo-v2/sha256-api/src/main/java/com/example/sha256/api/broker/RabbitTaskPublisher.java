package com.example.sha256.api.broker;

import com.example.sha256.common.model.Sha256TaskMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "sha256.broker", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitTaskPublisher implements TaskPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;
    private final long confirmTimeoutMs;

    public RabbitTaskPublisher(RabbitTemplate rabbitTemplate,
                               ObjectMapper objectMapper,
                               @Value("${sha256.rabbit.exchange:sha256.exchange}") String exchange,
                               @Value("${sha256.rabbit.routing-key:sha256.calculate}") String routingKey,
                               @Value("${sha256.rabbit.publisher-confirm-timeout-ms:10000}") long confirmTimeoutMs) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    @Override
    public Mono<Void> publish(Sha256TaskMessage message) {
        return Mono.fromRunnable(() -> {
                    CorrelationData correlationData = new CorrelationData(message.taskId());
                    rabbitTemplate.convertAndSend(exchange, routingKey, toJson(message), correlationData);
                    try {
                        CorrelationData.Confirm confirm = correlationData.getFuture()
                                .get(confirmTimeoutMs, TimeUnit.MILLISECONDS);
                        if (!confirm.isAck()) {
                            throw new IllegalStateException("RabbitMQ publisher confirm NACK: " + confirm.getReason());
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("RabbitMQ publisher confirm failed", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public String brokerName() {
        return "rabbitmq";
    }

    private String toJson(Sha256TaskMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize RabbitMQ task message", e);
        }
    }
}

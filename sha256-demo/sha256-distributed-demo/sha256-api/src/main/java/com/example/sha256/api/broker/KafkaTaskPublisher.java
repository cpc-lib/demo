package com.example.sha256.api.broker;

import com.example.sha256.common.model.Sha256TaskMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "sha256.broker", havingValue = "kafka")
public class KafkaTaskPublisher implements TaskPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaTaskPublisher(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              @Value("${sha256.kafka.topic:sha256.tasks}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public Mono<Void> publish(Sha256TaskMessage message) {
        return Mono.fromFuture(kafkaTemplate.send(topic, message.taskId(), toJson(message)))
                .then();
    }

    @Override
    public String brokerName() {
        return "kafka";
    }

    private String toJson(Sha256TaskMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Kafka task message", e);
        }
    }
}

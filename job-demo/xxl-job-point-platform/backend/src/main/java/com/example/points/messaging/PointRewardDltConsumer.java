package com.example.points.messaging;

import com.example.points.domain.PointRewardCommand;
import com.example.points.repository.FailureRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PointRewardDltConsumer {
    private final FailureRepository repo;

    public PointRewardDltConsumer(FailureRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = PointRewardProducer.TOPIC + ".DLT", groupId = "point-reward-dlt-consumer")
    public void consume(ConsumerRecord<String, PointRewardCommand> r) {
        String error = "Kafka重试耗尽";
        var h = r.headers().lastHeader("kafka_dlt-exception-message");
        if (h != null) error = new String(h.value(), StandardCharsets.UTF_8);
        repo.upsert(r.value(), error);
    }
}

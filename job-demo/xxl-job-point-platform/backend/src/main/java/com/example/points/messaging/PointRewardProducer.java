package com.example.points.messaging;

import com.example.points.domain.PointRewardCommand;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PointRewardProducer {
    public static final String TOPIC = "point.reward.command";
    private final KafkaTemplate<String, PointRewardCommand> kafka;

    public PointRewardProducer(KafkaTemplate<String, PointRewardCommand> kafka) {
        this.kafka = kafka;
    }

    public void send(PointRewardCommand c) {
        try {
            kafka.send(TOPIC, c.userId().toString(), c).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Kafka发送失败 bizNo=" + c.bizNo(), e);
        }
    }

    public void sendBatch(List<PointRewardCommand> list) {
        var fs = list.stream().map(c -> kafka.send(TOPIC, c.userId().toString(), c)).toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(fs).get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Kafka批量发送失败", e);
        }
    }
}

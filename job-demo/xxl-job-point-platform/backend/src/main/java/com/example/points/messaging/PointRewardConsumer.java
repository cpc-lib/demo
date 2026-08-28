package com.example.points.messaging;

import com.example.points.domain.PointRewardCommand;
import com.example.points.repository.FailureRepository;
import com.example.points.service.PointRewardApplicationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PointRewardConsumer {
    private final PointRewardApplicationService service;
    private final FailureRepository failures;

    public PointRewardConsumer(PointRewardApplicationService service, FailureRepository failures) {
        this.service = service;
        this.failures = failures;
    }

    @KafkaListener(topics = PointRewardProducer.TOPIC, groupId = "point-reward-consumer")
    public void consume(PointRewardCommand c) {
        service.reward(c);
        failures.resolved(c.bizNo());
    }
}

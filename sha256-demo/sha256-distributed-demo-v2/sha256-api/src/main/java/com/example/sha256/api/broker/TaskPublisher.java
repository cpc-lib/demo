package com.example.sha256.api.broker;

import com.example.sha256.common.model.Sha256TaskMessage;
import reactor.core.publisher.Mono;

public interface TaskPublisher {
    Mono<Void> publish(Sha256TaskMessage message);
    String brokerName();
}

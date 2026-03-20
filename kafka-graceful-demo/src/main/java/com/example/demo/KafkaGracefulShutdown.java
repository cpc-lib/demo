package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Slf4j
@Component
public class KafkaGracefulShutdown {

    @Resource
    private KafkaListenerEndpointRegistry registry;

    @PreDestroy
    public void shutdown() {
        log.info("Stopping Kafka listeners...");
        registry.stop();
        log.info("Kafka listeners stopped");
    }
}

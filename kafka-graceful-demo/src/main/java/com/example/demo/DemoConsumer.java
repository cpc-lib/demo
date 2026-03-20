package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoConsumer {

    @KafkaListener(topics = "demo-topic")
    public void onMessage(ConsumerRecord<String, String> record,
                          Acknowledgment ack) throws InterruptedException {
        log.info("Consume message: {}", record.value());

        // 模拟慢处理
        Thread.sleep(5000);

        ack.acknowledge();
        log.info("Message finished");
    }
}

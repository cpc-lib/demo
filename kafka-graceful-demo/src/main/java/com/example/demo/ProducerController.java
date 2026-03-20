package com.example.demo;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class ProducerController {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/send")
    public String send() throws InterruptedException {
        Thread.sleep(5000);
        kafkaTemplate.send("demo-topic", "hello-" + System.currentTimeMillis());
        return "sent";
    }
}

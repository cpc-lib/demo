package com.example.articledelay.infrastructure.kafka;

import com.example.articledelay.config.DelayPublishProperties;
import com.example.articledelay.domain.DelayTask;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class ArticlePublishProducer {

    private final KafkaTemplate<String, ArticlePublishEvent> kafkaTemplate;
    private final DelayPublishProperties properties;

    public ArticlePublishProducer(
            KafkaTemplate<String, ArticlePublishEvent> kafkaTemplate,
            DelayPublishProperties properties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public CompletableFuture<SendResult<String, ArticlePublishEvent>> send(DelayTask task) {
        ArticlePublishEvent event = new ArticlePublishEvent(
                task.articleId(),
                task.scheduleVersion(),
                task.publishAt()
        );
        return kafkaTemplate.send(properties.getTopic(), task.articleId().toString(), event);
    }
}

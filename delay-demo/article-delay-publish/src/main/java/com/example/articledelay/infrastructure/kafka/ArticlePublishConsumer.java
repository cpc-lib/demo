package com.example.articledelay.infrastructure.kafka;

import com.example.articledelay.config.DelayPublishProperties;
import com.example.articledelay.domain.ArticleRepository;
import com.example.articledelay.domain.ArticleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class ArticlePublishConsumer {

    private static final Logger log = LoggerFactory.getLogger(ArticlePublishConsumer.class);

    private final ArticleRepository articleRepository;
    private final Clock clock;
    private final DelayPublishProperties properties;

    public ArticlePublishConsumer(
            ArticleRepository articleRepository,
            Clock clock,
            DelayPublishProperties properties
    ) {
        this.articleRepository = articleRepository;
        this.clock = clock;
        this.properties = properties;
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            autoCreateTopics = "true",
            numPartitions = "3",
            replicationFactor = "1",
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(
            topics = "${app.delay.topic:article.publish}",
            groupId = "${spring.kafka.consumer.group-id:article-publish-consumer}"
    )
    @Transactional
    public void consume(ArticlePublishEvent event) {
        Instant now = clock.instant();
        int updated = articleRepository.tryPublish(
                event.articleId(),
                event.scheduleVersion(),
                now,
                ArticleStatus.SCHEDULED,
                ArticleStatus.PUBLISHED
        );

        if (updated == 0) {
            log.info(
                    "Ignore duplicate/stale article publish event: articleId={}, scheduleVersion={}",
                    event.articleId(),
                    event.scheduleVersion()
            );
            return;
        }

        log.info(
                "Article published: articleId={}, scheduleVersion={}, topic={}",
                event.articleId(),
                event.scheduleVersion(),
                properties.getTopic()
        );
    }

    @DltHandler
    public void onDlt(ArticlePublishEvent event) {
        log.error(
                "Article publish event reached DLT: articleId={}, scheduleVersion={}",
                event.articleId(),
                event.scheduleVersion()
        );
    }
}

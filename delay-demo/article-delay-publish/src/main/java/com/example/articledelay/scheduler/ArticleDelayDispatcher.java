package com.example.articledelay.scheduler;

import com.example.articledelay.config.DelayPublishProperties;
import com.example.articledelay.domain.DelayTask;
import com.example.articledelay.infrastructure.kafka.ArticlePublishProducer;
import com.example.articledelay.infrastructure.redis.RedisArticleDelayQueue;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class ArticleDelayDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ArticleDelayDispatcher.class);

    private final RedisArticleDelayQueue delayQueue;
    private final ArticlePublishProducer producer;
    private final DelayPublishProperties properties;
    private final Clock clock;
    private final Counter claimedCounter;
    private final Counter sentCounter;
    private final Counter sendFailedCounter;

    public ArticleDelayDispatcher(
            RedisArticleDelayQueue delayQueue,
            ArticlePublishProducer producer,
            DelayPublishProperties properties,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.delayQueue = delayQueue;
        this.producer = producer;
        this.properties = properties;
        this.clock = clock;
        this.claimedCounter = meterRegistry.counter("article.delay.claimed");
        this.sentCounter = meterRegistry.counter("article.delay.kafka.sent");
        this.sendFailedCounter = meterRegistry.counter("article.delay.kafka.failed");
    }

    @Scheduled(fixedDelayString = "${app.delay.dispatch-interval:500ms}")
    public void dispatch() {
        Instant now = clock.instant();
        List<DelayTask> tasks = delayQueue.claimDue(now, properties.getBatchSize(), properties.getLease());
        if (tasks.isEmpty()) {
            return;
        }

        claimedCounter.increment(tasks.size());
        for (DelayTask task : tasks) {
            producer.send(task).whenComplete((result, error) -> {
                if (error == null) {
                    try {
                        delayQueue.ack(task);
                        sentCounter.increment();
                    } catch (RuntimeException redisError) {
                        log.error("Kafka ACK received but Redis task ACK failed; lease recovery may redeliver: {}",
                                task.member(), redisError);
                    }
                    return;
                }

                sendFailedCounter.increment();
                Instant retryAt = clock.instant().plus(properties.getRetryBackoff());
                try {
                    delayQueue.nack(task, retryAt);
                } catch (RuntimeException redisError) {
                    log.error("Failed to requeue task after Kafka send failure: {}", task.member(), redisError);
                }
                log.error("Failed to send article publish event to Kafka: {}", task.member(), error);
            });
        }
    }
}

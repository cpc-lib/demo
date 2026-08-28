package com.example.articledelay.scheduler;

import com.example.articledelay.config.DelayPublishProperties;
import com.example.articledelay.domain.DelayTask;
import com.example.articledelay.infrastructure.redis.RedisArticleDelayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
public class RedisProcessingRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RedisProcessingRecoveryScheduler.class);

    private final RedisArticleDelayQueue delayQueue;
    private final DelayPublishProperties properties;
    private final Clock clock;

    public RedisProcessingRecoveryScheduler(
            RedisArticleDelayQueue delayQueue,
            DelayPublishProperties properties,
            Clock clock
    ) {
        this.delayQueue = delayQueue;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.delay.recovery-interval:5s}")
    public void recoverExpiredClaims() {
        List<DelayTask> recovered = delayQueue.recoverExpired(clock.instant(), properties.getBatchSize());
        if (!recovered.isEmpty()) {
            log.warn("Recovered {} expired Redis processing leases", recovered.size());
        }
    }
}

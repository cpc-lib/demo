package com.example.articledelay.application;

import com.example.articledelay.infrastructure.redis.RedisArticleDelayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RedisScheduleSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(RedisScheduleSynchronizer.class);

    private final RedisArticleDelayQueue delayQueue;

    public RedisScheduleSynchronizer(RedisArticleDelayQueue delayQueue) {
        this.delayQueue = delayQueue;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScheduleChanged(ArticleScheduleChangedEvent event) {
        try {
            if (event.oldTask() != null) {
                delayQueue.removeScheduled(event.oldTask());
            }
            if (event.newTask() != null) {
                delayQueue.enqueueIfUnknown(event.newTask());
            }
        } catch (RuntimeException ex) {
            // MySQL is the source of truth. The compensator will repair Redis later.
            log.error("Failed to synchronize article schedule to Redis; DB compensator will repair it", ex);
        }
    }
}

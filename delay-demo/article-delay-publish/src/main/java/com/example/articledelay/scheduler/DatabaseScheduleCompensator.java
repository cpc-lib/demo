package com.example.articledelay.scheduler;

import com.example.articledelay.config.DelayPublishProperties;
import com.example.articledelay.domain.Article;
import com.example.articledelay.domain.ArticleRepository;
import com.example.articledelay.domain.ArticleStatus;
import com.example.articledelay.domain.DelayTask;
import com.example.articledelay.infrastructure.redis.RedisArticleDelayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class DatabaseScheduleCompensator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseScheduleCompensator.class);

    private final ArticleRepository articleRepository;
    private final RedisArticleDelayQueue delayQueue;
    private final DelayPublishProperties properties;
    private final Clock clock;

    public DatabaseScheduleCompensator(
            ArticleRepository articleRepository,
            RedisArticleDelayQueue delayQueue,
            DelayPublishProperties properties,
            Clock clock
    ) {
        this.articleRepository = articleRepository;
        this.delayQueue = delayQueue;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.delay.compensation-interval:30s}")
    @Transactional(readOnly = true)
    public void compensate() {
        Instant horizon = clock.instant().plus(properties.getCompensationLookAhead());
        List<Article> candidates = articleRepository
                .findByStatusAndPublishTimeLessThanEqualOrderByPublishTimeAsc(
                        ArticleStatus.SCHEDULED,
                        horizon,
                        PageRequest.of(0, properties.getCompensationBatchSize())
                );

        int repaired = 0;
        for (Article article : candidates) {
            if (article.getPublishTime() == null) {
                continue;
            }
            DelayTask task = new DelayTask(
                    article.getId(),
                    article.getScheduleVersion(),
                    article.getPublishTime()
            );
            if (delayQueue.enqueueIfUnknown(task)) {
                repaired++;
            }
        }

        if (repaired > 0) {
            log.warn("Database compensator restored {} missing Redis delay tasks", repaired);
        }
    }
}

package com.example.articledelay.infrastructure.kafka;

import com.example.articledelay.config.DelayPublishProperties;
import com.example.articledelay.domain.ArticleRepository;
import com.example.articledelay.domain.ArticleStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticlePublishConsumerTest {

    @Test
    void duplicateOrStaleMessageShouldBeHarmless() {
        ArticleRepository repository = mock(ArticleRepository.class);
        Instant now = Instant.parse("2026-08-28T10:00:00Z");
        when(repository.tryPublish(1L, 3L, now, ArticleStatus.SCHEDULED, ArticleStatus.PUBLISHED))
                .thenReturn(0);

        ArticlePublishConsumer consumer = new ArticlePublishConsumer(
                repository,
                Clock.fixed(now, ZoneOffset.UTC),
                new DelayPublishProperties()
        );

        consumer.consume(new ArticlePublishEvent(1L, 3L, now.minusSeconds(10)));

        verify(repository).tryPublish(1L, 3L, now, ArticleStatus.SCHEDULED, ArticleStatus.PUBLISHED);
    }
}

package com.example.articledelay.application;

import com.example.articledelay.domain.Article;
import com.example.articledelay.domain.ArticleRepository;
import com.example.articledelay.domain.ArticleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleServiceTest {

    private ArticleRepository repository;
    private ApplicationEventPublisher publisher;
    private ArticleService service;
    private final Instant now = Instant.parse("2026-08-28T10:00:00Z");

    @BeforeEach
    void setUp() {
        repository = mock(ArticleRepository.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new ArticleService(repository, publisher, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void shouldScheduleWithVersionAndPublishEvent() throws Exception {
        Article article = Article.draft("Redis + Kafka", "body");
        setId(article, 42L);
        when(repository.findForUpdateById(42L)).thenReturn(Optional.of(article));
        Instant publishAt = now.plusSeconds(60);

        var response = service.schedule(42L, publishAt);

        assertThat(response.status()).isEqualTo(ArticleStatus.SCHEDULED);
        assertThat(response.scheduleVersion()).isEqualTo(1L);
        assertThat(response.publishAt()).isEqualTo(publishAt);

        ArgumentCaptor<ArticleScheduleChangedEvent> captor = ArgumentCaptor.forClass(ArticleScheduleChangedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().oldTask()).isNull();
        assertThat(captor.getValue().newTask().articleId()).isEqualTo(42L);
        assertThat(captor.getValue().newTask().scheduleVersion()).isEqualTo(1L);
    }

    @Test
    void shouldRejectPastSchedule() {
        assertThatThrownBy(() -> service.schedule(42L, now.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void setId(Article article, long id) throws Exception {
        Field field = Article.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(article, id);
    }
}

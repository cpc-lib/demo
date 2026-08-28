package com.example.articledelay.infrastructure.persistence;

import com.example.articledelay.domain.Article;
import com.example.articledelay.domain.ArticleRepository;
import com.example.articledelay.domain.ArticleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:article_delay_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC"
})
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository repository;

    @Test
    void searchShouldFilterByStatusAndKeyword() {
        Article draft = repository.saveAndFlush(Article.draft("Redis delay design", "draft body"));
        Article scheduled = Article.draft("Kafka publish pipeline", "scheduled body");
        scheduled.schedule(Instant.parse("2026-08-28T11:00:00Z"));
        repository.saveAndFlush(scheduled);

        var page = repository.search("Kafka", ArticleStatus.SCHEDULED, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Article::getTitle)
                .containsExactly("Kafka publish pipeline");
        assertThat(draft.getStatus()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    void publishUpdateShouldBeIdempotent() {
        Instant publishAt = Instant.parse("2026-08-28T09:59:00Z");
        Instant now = Instant.parse("2026-08-28T10:00:00Z");

        Article article = Article.draft("title", "content");
        article.schedule(publishAt);
        article = repository.saveAndFlush(article);

        int first = repository.tryPublish(
                article.getId(), article.getScheduleVersion(), now,
                ArticleStatus.SCHEDULED, ArticleStatus.PUBLISHED
        );
        int second = repository.tryPublish(
                article.getId(), article.getScheduleVersion(), now,
                ArticleStatus.SCHEDULED, ArticleStatus.PUBLISHED
        );

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(repository.findById(article.getId()).orElseThrow().getStatus())
                .isEqualTo(ArticleStatus.PUBLISHED);
    }
}

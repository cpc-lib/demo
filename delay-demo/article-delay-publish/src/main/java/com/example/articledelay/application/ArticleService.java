package com.example.articledelay.application;

import com.example.articledelay.api.ArticlePageResponse;
import com.example.articledelay.api.ArticleResponse;
import com.example.articledelay.domain.Article;
import com.example.articledelay.domain.ArticleRepository;
import com.example.articledelay.domain.ArticleStatus;
import com.example.articledelay.domain.DelayTask;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ArticleService(
            ArticleRepository articleRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.articleRepository = articleRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ArticleResponse createDraft(String title, String content) {
        Article article = articleRepository.saveAndFlush(Article.draft(title, content));
        return ArticleResponse.from(article);
    }


    @Transactional(readOnly = true)
    public ArticlePageResponse list(String keyword, ArticleStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

        Page<ArticleResponse> result = articleRepository.search(
                        normalizedKeyword,
                        status,
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(ArticleResponse::from);

        return ArticlePageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ArticleResponse get(Long id) {
        return articleRepository.findById(id)
                .map(ArticleResponse::from)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }

    @Transactional
    public ArticleResponse schedule(Long id, Instant publishAt) {
        Instant now = clock.instant();
        if (publishAt == null || !publishAt.isAfter(now)) {
            throw new IllegalArgumentException("publishAt must be later than current time");
        }

        Article article = articleRepository.findForUpdateById(id)
                .orElseThrow(() -> new ArticleNotFoundException(id));

        DelayTask oldTask = toCurrentDelayTask(article);
        article.schedule(publishAt);
        articleRepository.flush();
        DelayTask newTask = new DelayTask(article.getId(), article.getScheduleVersion(), publishAt);

        eventPublisher.publishEvent(new ArticleScheduleChangedEvent(oldTask, newTask));
        return ArticleResponse.from(article);
    }

    @Transactional
    public ArticleResponse cancelSchedule(Long id) {
        Article article = articleRepository.findForUpdateById(id)
                .orElseThrow(() -> new ArticleNotFoundException(id));

        DelayTask oldTask = toCurrentDelayTask(article);
        article.cancelSchedule();
        articleRepository.flush();

        if (oldTask != null) {
            eventPublisher.publishEvent(new ArticleScheduleChangedEvent(oldTask, null));
        }
        return ArticleResponse.from(article);
    }

    private DelayTask toCurrentDelayTask(Article article) {
        if (article.getStatus() != ArticleStatus.SCHEDULED || article.getPublishTime() == null) {
            return null;
        }
        return new DelayTask(article.getId(), article.getScheduleVersion(), article.getPublishTime());
    }
}

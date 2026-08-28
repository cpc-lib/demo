package com.example.articledelay.api;

import com.example.articledelay.domain.Article;
import com.example.articledelay.domain.ArticleStatus;

import java.time.Instant;

public record ArticleResponse(
        Long id,
        String title,
        String content,
        ArticleStatus status,
        Instant publishAt,
        Instant publishedAt,
        long scheduleVersion,
        long rowVersion,
        Instant createdAt,
        Instant updatedAt
) {
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getStatus(),
                article.getPublishTime(),
                article.getPublishedAt(),
                article.getScheduleVersion(),
                article.getRowVersion(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}

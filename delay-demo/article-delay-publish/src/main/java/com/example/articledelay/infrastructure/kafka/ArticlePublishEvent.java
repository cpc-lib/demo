package com.example.articledelay.infrastructure.kafka;

import java.time.Instant;

public record ArticlePublishEvent(Long articleId, long scheduleVersion, Instant publishAt) {
}

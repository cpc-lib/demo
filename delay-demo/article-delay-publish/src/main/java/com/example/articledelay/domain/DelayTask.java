package com.example.articledelay.domain;

import java.time.Instant;
import java.util.Objects;

public record DelayTask(Long articleId, long scheduleVersion, Instant publishAt) {

    public DelayTask {
        Objects.requireNonNull(articleId, "articleId");
        Objects.requireNonNull(publishAt, "publishAt");
        if (articleId <= 0) {
            throw new IllegalArgumentException("articleId must be positive");
        }
        if (scheduleVersion <= 0) {
            throw new IllegalArgumentException("scheduleVersion must be positive");
        }
    }

    public String member() {
        return articleId + ":" + scheduleVersion + ":" + publishAt.toEpochMilli();
    }

    public static DelayTask fromMember(String member) {
        if (member == null || member.isBlank()) {
            throw new IllegalArgumentException("Redis delay member must not be blank");
        }
        String[] parts = member.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid Redis delay member: " + member);
        }
        try {
            return new DelayTask(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1]),
                    Instant.ofEpochMilli(Long.parseLong(parts[2]))
            );
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid Redis delay member: " + member, ex);
        }
    }
}

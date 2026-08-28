package com.example.articledelay.api;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ScheduleArticleRequest(@NotNull OffsetDateTime publishAt) {
}

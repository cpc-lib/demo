package com.example.articledelay.application;

import com.example.articledelay.domain.DelayTask;

public record ArticleScheduleChangedEvent(DelayTask oldTask, DelayTask newTask) {
}

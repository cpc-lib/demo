package com.example.articledelay.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelayTaskTest {

    @Test
    void shouldRoundTripRedisMember() {
        DelayTask task = new DelayTask(10001L, 7L, Instant.parse("2026-08-28T12:00:00Z"));

        DelayTask decoded = DelayTask.fromMember(task.member());

        assertThat(decoded).isEqualTo(task);
    }

    @Test
    void shouldRejectInvalidMember() {
        assertThatThrownBy(() -> DelayTask.fromMember("broken"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

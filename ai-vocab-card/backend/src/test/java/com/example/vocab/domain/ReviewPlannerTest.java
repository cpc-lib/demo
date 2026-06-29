package com.example.vocab.domain;

import com.example.vocab.entity.review.UserWordBook;
import com.example.vocab.service.review.ReviewPlanner;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewPlannerTest {
    @Test
    void forgotShouldScheduleOneHourLaterAndResetMastery() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 29, 10, 0);
        UserWordBook book = new UserWordBook();
        book.setMasteryLevel(3);
        book.setReviewCount(5);
        book.setEaseFactor(2.5D);

        ReviewPlanner.plan(book, 0, now);

        assertThat(book.getMasteryLevel()).isZero();
        assertThat(book.getReviewCount()).isEqualTo(6);
        assertThat(book.getNextReviewTime()).isEqualTo(now.plusHours(1));
        assertThat(book.getEaseFactor()).isLessThan(2.5D);
    }

    @Test
    void rememberedShouldIncreaseMasteryAndScheduleFutureReview() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 29, 10, 0);
        UserWordBook book = new UserWordBook();
        book.setMasteryLevel(2);
        book.setReviewCount(1);
        book.setEaseFactor(2.5D);

        ReviewPlanner.plan(book, 2, now);

        assertThat(book.getMasteryLevel()).isEqualTo(3);
        assertThat(book.getReviewCount()).isEqualTo(2);
        assertThat(book.getNextReviewTime()).isEqualTo(now.plusDays(7));
        assertThat(book.getEaseFactor()).isGreaterThan(2.5D);
    }
}

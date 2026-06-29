package com.example.vocab.service.review;

import com.example.vocab.entity.review.UserWordBook;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class Sm2ReviewPlannerTest {
    @Test
    void forgotShouldReviewAgainSoonAndDecreaseEase() {
        UserWordBook book = newBook();
        LocalDateTime now = LocalDateTime.of(2026, 6, 29, 10, 0);
        ReviewPlanner.plan(book, 0, now);
        assertEquals(0, book.getMasteryLevel());
        assertTrue(book.getEaseFactor() < 2.5D);
        assertEquals(now.plusHours(1), book.getNextReviewTime());
    }

    @Test
    void rememberedSeveralTimesShouldIncreaseInterval() {
        UserWordBook book = newBook();
        LocalDateTime now = LocalDateTime.of(2026, 6, 29, 10, 0);
        ReviewPlanner.plan(book, 2, now);
        LocalDateTime first = book.getNextReviewTime();
        ReviewPlanner.plan(book, 2, now.plusDays(1));
        assertTrue(book.getNextReviewTime().isAfter(first));
        assertTrue(book.getMasteryLevel() >= 2);
    }

    private UserWordBook newBook() {
        UserWordBook book = new UserWordBook();
        book.setUserId(1L);
        book.setWordCardId(1L);
        book.setMasteryLevel(0);
        book.setReviewCount(0);
        book.setEaseFactor(2.5D);
        return book;
    }
}

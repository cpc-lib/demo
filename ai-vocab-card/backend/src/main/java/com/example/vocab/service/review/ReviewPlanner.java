package com.example.vocab.service.review;

import com.example.vocab.entity.review.UserWordBook;
import java.time.LocalDateTime;

/**
 * SM-2 inspired scheduler.
 * result: 0 = forgot, 1 = vague, 2 = remembered.
 * We map this compact UX result to SM-2 quality: 2, 4, 5.
 */
public final class ReviewPlanner {
    private ReviewPlanner() {}

    public static UserWordBook plan(UserWordBook book, int result, LocalDateTime now) {
        int oldCount = book.getReviewCount() == null ? 0 : book.getReviewCount();
        int oldLevel = book.getMasteryLevel() == null ? 0 : book.getMasteryLevel();
        double oldEase = book.getEaseFactor() == null ? 2.5D : book.getEaseFactor();
        int quality = switch (result) {
            case 0 -> 2;
            case 1 -> 4;
            default -> 5;
        };

        double newEase = oldEase + (0.1D - (5 - quality) * (0.08D + (5 - quality) * 0.02D));
        newEase = Math.max(1.3D, Math.min(3.2D, newEase));

        int newLevel;
        int intervalDays;
        if (quality < 3) {
            newLevel = 0;
            intervalDays = 0;
            book.setNextReviewTime(now.plusHours(1));
        } else {
            newLevel = Math.min(8, oldLevel + 1);
            if (newLevel == 1) intervalDays = 1;
            else if (newLevel == 2) intervalDays = 3;
            else if (newLevel == 3) intervalDays = 7;
            else intervalDays = Math.max(1, (int) Math.round(previousIntervalDays(oldLevel) * newEase));
            book.setNextReviewTime(now.plusDays(intervalDays));
        }

        book.setMasteryLevel(newLevel);
        book.setReviewCount(oldCount + 1);
        book.setEaseFactor(round2(newEase));
        book.setLastReviewTime(now);
        return book;
    }

    public static int intervalDays(UserWordBook book, LocalDateTime now) {
        if (book.getNextReviewTime() == null) return 0;
        long hours = java.time.Duration.between(now, book.getNextReviewTime()).toHours();
        return (int) Math.max(0, Math.round(hours / 24.0D));
    }

    private static int previousIntervalDays(int level) {
        return switch (level) {
            case 0, 1 -> 1;
            case 2 -> 3;
            case 3 -> 7;
            case 4 -> 15;
            case 5 -> 30;
            case 6 -> 60;
            default -> 90;
        };
    }

    private static double round2(double v) { return Math.round(v * 100D) / 100D; }
}

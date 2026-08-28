package com.example.points.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class PointBizNoGenerator {
    private static final DateTimeFormatter F = DateTimeFormatter.BASIC_ISO_DATE;

    private PointBizNoGenerator() {
    }

    public static String dailyReward(long userId, LocalDate date) {
        return "DAILY_REWARD:" + userId + ":" + F.format(date);
    }
}

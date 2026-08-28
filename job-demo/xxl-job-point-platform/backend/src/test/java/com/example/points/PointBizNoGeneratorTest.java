package com.example.points;

import com.example.points.domain.PointBizNoGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PointBizNoGeneratorTest {
    @Test
    void sameBusinessMustHaveSameBizNo() {
        var d = LocalDate.of(2026, 8, 28);
        assertEquals("DAILY_REWARD:10001:20260828", PointBizNoGenerator.dailyReward(10001, d));
        assertEquals(PointBizNoGenerator.dailyReward(10001, d), PointBizNoGenerator.dailyReward(10001, d));
    }
}

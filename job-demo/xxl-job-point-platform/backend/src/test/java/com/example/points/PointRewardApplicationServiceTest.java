package com.example.points;

import com.example.points.domain.PointRewardCommand;
import com.example.points.domain.PointRewardResult;
import com.example.points.service.PointRewardApplicationService;
import com.example.points.service.PointRewardTransactionalService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PointRewardApplicationServiceTest {
    @Test
    void duplicateKeyMeansDuplicateBusinessNotSecondCredit() {
        var tx = mock(PointRewardTransactionalService.class);
        var service = new PointRewardApplicationService(tx);
        var cmd = new PointRewardCommand(1L, 10001L, "DAILY_REWARD:10001:20260828", "DAILY_REWARD", 10L, LocalDate.of(2026, 8, 28));
        doThrow(new DuplicateKeyException("uk_point_biz_no")).when(tx).reward(cmd);
        assertEquals(PointRewardResult.DUPLICATE, service.reward(cmd));
        verify(tx, times(1)).reward(cmd);
    }
}

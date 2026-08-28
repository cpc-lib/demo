package com.example.points.domain;

import java.time.LocalDate;

public record PointRewardCommand(Long batchId, Long userId, String bizNo, String bizType, Long points,
                                 LocalDate rewardDate) {
}

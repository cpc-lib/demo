package com.example.orderjob.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSnapshot(
        Long id,
        String orderNo,
        OrderStatus status,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime expireTime,
        LocalDateTime payTime,
        LocalDateTime closeTime,
        Integer version,
        String closeReason
) {
}

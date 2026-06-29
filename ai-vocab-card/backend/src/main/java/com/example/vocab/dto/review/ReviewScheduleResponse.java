package com.example.vocab.dto.review;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewScheduleResponse {
    private Long userId;
    private Long wordCardId;
    private Integer masteryLevel;
    private Integer reviewCount;
    private Double easeFactor;
    private LocalDateTime nextReviewTime;
}

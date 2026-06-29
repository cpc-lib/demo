package com.example.vocab.dto.review;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubmitReviewRequest {
    private Long userId;
    @NotNull
    private Long wordCardId;
    /** 0=forgot, 1=vague, 2=remembered */
    @NotNull
    @Min(0)
    @Max(2)
    private Integer result;
}

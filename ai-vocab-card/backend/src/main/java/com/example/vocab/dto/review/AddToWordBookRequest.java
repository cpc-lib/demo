package com.example.vocab.dto.review;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToWordBookRequest {
    private Long userId;
    @NotNull
    private Long wordCardId;
}

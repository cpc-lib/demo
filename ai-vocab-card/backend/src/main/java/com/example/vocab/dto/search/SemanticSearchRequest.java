package com.example.vocab.dto.search;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SemanticSearchRequest {
    @NotBlank
    private String query;
    private Integer topK;
}

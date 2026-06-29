package com.example.vocab.dto.prompt;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromptTemplateDTO {
    private Long id;
    @NotBlank
    private String code;
    @NotBlank
    private String version;
    private String title;
    @NotBlank
    private String content;
    private Integer enabled;
}

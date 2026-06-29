package com.example.vocab.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class SlangDTO { @NotBlank private String phrase; private String meaning; private String example; }

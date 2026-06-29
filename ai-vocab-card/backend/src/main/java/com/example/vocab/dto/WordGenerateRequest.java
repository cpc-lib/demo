package com.example.vocab.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class WordGenerateRequest { @NotBlank private String word; }

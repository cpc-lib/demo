package com.example.vocab.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class ExampleDTO { @NotBlank private String sentence; private String translation; private String scene; }

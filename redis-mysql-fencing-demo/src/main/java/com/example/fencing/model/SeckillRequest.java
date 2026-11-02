package com.example.fencing.model;

import jakarta.validation.constraints.NotBlank;

public record SeckillRequest(@NotBlank String userId) { }

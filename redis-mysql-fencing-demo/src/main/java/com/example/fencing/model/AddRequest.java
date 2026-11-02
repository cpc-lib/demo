package com.example.fencing.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddRequest(@NotNull @Min(1) Long delta) { }

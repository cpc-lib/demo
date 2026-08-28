package cc.ivera.ragdemo.controller;

import jakarta.validation.constraints.NotBlank;

public record IngestTextRequest(@NotBlank String text) {
}

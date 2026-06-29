package com.example.vocab.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long userId;
    private String username;
    private String token;
}

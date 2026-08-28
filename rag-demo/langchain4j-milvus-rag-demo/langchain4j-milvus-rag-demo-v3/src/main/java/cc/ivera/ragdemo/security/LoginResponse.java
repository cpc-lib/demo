package cc.ivera.ragdemo.security;

import cc.ivera.ragdemo.tenant.CurrentUserResponse;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        Instant issuedAt,
        Instant expiresAt,
        CurrentUserResponse currentUser
) {
}

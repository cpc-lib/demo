package cc.ivera.ragdemo.security;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}

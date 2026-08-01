package cc.ivera.ragdemo.admin;

public record UserPasswordResetRequest(
        String password,
        Boolean mustChangePassword
) {
}

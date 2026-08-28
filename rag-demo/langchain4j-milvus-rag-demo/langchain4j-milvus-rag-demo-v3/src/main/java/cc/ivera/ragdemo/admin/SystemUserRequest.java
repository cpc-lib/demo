package cc.ivera.ragdemo.admin;

public record SystemUserRequest(
        Long tenantId,
        String externalUserId,
        String username,
        String displayName,
        String email,
        String password,
        Integer status,
        Boolean mustChangePassword
) {
}

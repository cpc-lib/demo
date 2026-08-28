package cc.ivera.ragdemo.security;

public record LoginRequest(
        Long tenantId,
        String tenantCode,
        String tenant,
        String account,
        String password,
        String loginType
) {

    public LoginRequest(Long tenantId, String tenantCode, String account, String password) {
        this(tenantId, tenantCode, null, account, password, null);
    }
}

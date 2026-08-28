package cc.ivera.ragdemo.tenant;

import java.time.Instant;

public record TenantContext(
        Long tenantId,
        String tenantExternalId,
        UserContext user,
        String requestId,
        String sourceIp,
        boolean systemContext,
        boolean impersonating,
        Long operatorTenantId,
        String impersonationReason,
        Instant createdAt
) {

    public TenantContext {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String operatorUserId() {
        return user == null ? "system" : user.userId();
    }

    public boolean platformAdmin() {
        return user != null && user.platformAdmin();
    }
}

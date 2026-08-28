package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.tenant.UserContext;

import java.time.LocalDateTime;
import java.util.Collection;

public class AdminImpersonationPolicy {

    public void assertCanStart(UserContext user, Long currentTenantId, Long targetTenantId, String reason) {
        assertCanStart(user, currentTenantId, targetTenantId, reason, null);
    }

    public void assertCanStart(UserContext user,
                               Long currentTenantId,
                               Long targetTenantId,
                               String reason,
                               Collection<String> configuredAdminRoles) {
        if (!new AdminRolePolicy().isPlatformAdmin(user, configuredAdminRoles)) {
            throw new IllegalStateException("Only platform administrators can start impersonation");
        }
        if (targetTenantId == null || targetTenantId < 0) {
            throw new IllegalArgumentException("targetTenantId is required");
        }
        if (targetTenantId.equals(currentTenantId)) {
            throw new IllegalArgumentException("targetTenantId must differ from operator tenant");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("impersonation reason is required");
        }
    }

    public boolean active(LocalDateTime expiresAt, LocalDateTime revokedAt, LocalDateTime now) {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(now == null ? LocalDateTime.now() : now);
    }
}

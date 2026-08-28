package cc.ivera.ragdemo.admin;

public record AdminImpersonationRequest(
        Long targetTenantId,
        String reason,
        Integer ttlMinutes
) {
}

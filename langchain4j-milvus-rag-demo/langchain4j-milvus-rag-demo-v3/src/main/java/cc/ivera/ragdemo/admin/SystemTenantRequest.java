package cc.ivera.ragdemo.admin;

public record SystemTenantRequest(
        String tenantCode,
        String tenantName,
        String externalId,
        Integer status
) {
}

package cc.ivera.ragdemo.tenant;

import java.util.List;

public record CurrentUserResponse(
        Long tenantId,
        Long operatorTenantId,
        String tenantExternalId,
        String userId,
        String displayName,
        List<String> roles,
        List<Long> authorizedKnowledgeBaseIds,
        List<String> permissionTags,
        boolean platformAdmin,
        boolean impersonating,
        String requestId,
        String sourceIp
) {
}

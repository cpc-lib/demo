package cc.ivera.ragdemo.tenant;

import java.util.List;

public record AuthenticatedIdentity(
        Long tenantId,
        String tenantExternalId,
        String userId,
        String userName,
        List<String> roles,
        List<Long> workspaceIds,
        List<Long> authorizedKnowledgeBaseIds,
        List<String> permissionTags,
        String requestId,
        Long impersonatedTenantId,
        String impersonationReason,
        String source
) {
}

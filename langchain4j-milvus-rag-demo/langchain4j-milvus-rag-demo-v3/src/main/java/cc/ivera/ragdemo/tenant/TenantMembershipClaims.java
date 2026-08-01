package cc.ivera.ragdemo.tenant;

import java.util.List;

public record TenantMembershipClaims(
        Long tenantId,
        List<String> roles,
        List<Long> workspaceIds,
        List<Long> knowledgeBaseIds,
        List<String> permissionTags
) {
}

package cc.ivera.ragdemo.permission;

import java.util.List;

public record KnowledgeBaseMemberRequest(
        String userId,
        String role,
        List<String> permissionTags
) {
}

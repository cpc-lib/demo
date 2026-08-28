package cc.ivera.ragdemo.tenant;

import java.util.*;
import java.util.stream.Collectors;

public record UserContext(
        String userId,
        String displayName,
        List<String> roles,
        List<Long> workspaceIds,
        List<Long> authorizedKnowledgeBaseIds,
        List<String> permissionTags
) {

    public UserContext {
        roles = normalizeStrings(roles);
        workspaceIds = normalizeLongs(workspaceIds);
        authorizedKnowledgeBaseIds = normalizeLongs(authorizedKnowledgeBaseIds);
        permissionTags = normalizeStrings(permissionTags);
    }

    public boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return roles.contains(normalized);
    }

    public boolean platformAdmin() {
        return hasRole("PLATFORM_ADMIN") || hasRole("SUPER_ADMIN");
    }

    public boolean platformAdmin(Collection<String> configuredAdminRoles) {
        if (configuredAdminRoles == null || configuredAdminRoles.isEmpty()) {
            return platformAdmin();
        }
        return configuredAdminRoles.stream().anyMatch(this::hasRole);
    }

    public Set<String> roleSet() {
        return roles.stream().collect(Collectors.toUnmodifiableSet());
    }

    private static List<String> normalizeStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static List<Long> normalizeLongs(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && value >= 0)
                .distinct()
                .toList();
    }
}

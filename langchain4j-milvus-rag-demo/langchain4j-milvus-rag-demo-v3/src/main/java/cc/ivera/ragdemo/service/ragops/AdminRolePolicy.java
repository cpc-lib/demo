package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.tenant.UserContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Component
public class AdminRolePolicy {

    private static final List<String> DEFAULT_ADMIN_ROLES = List.of("SUPER_ADMIN");

    public boolean isPlatformAdmin(UserContext user, Collection<String> configuredAdminRoles) {
        if (user == null) {
            return false;
        }
        return user.platformAdmin(adminRoles(configuredAdminRoles));
    }

    public String[] authorityNames(Collection<String> configuredAdminRoles) {
        return adminRoles(configuredAdminRoles).toArray(String[]::new);
    }

    private List<String> adminRoles(Collection<String> configuredAdminRoles) {
        List<String> roles = configuredAdminRoles == null ? List.of() : configuredAdminRoles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        return roles.isEmpty() ? DEFAULT_ADMIN_ROLES : roles;
    }
}

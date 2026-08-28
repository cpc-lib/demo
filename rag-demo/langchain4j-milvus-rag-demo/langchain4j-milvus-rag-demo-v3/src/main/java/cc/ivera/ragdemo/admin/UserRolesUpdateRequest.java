package cc.ivera.ragdemo.admin;

import java.util.List;

public record UserRolesUpdateRequest(
        List<String> roleCodes
) {
}

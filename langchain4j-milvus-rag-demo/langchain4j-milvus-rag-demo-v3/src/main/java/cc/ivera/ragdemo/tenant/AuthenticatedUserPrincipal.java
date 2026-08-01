package cc.ivera.ragdemo.tenant;

import java.security.Principal;

public record AuthenticatedUserPrincipal(UserContext userContext) implements Principal {

    @Override
    public String getName() {
        return userContext == null ? "anonymous" : userContext.userId();
    }
}

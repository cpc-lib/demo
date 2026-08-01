package cc.ivera.ragdemo.tenant;

import jakarta.servlet.http.HttpServletRequest;

public interface IdentityProvider {

    AuthenticatedIdentity authenticate(HttpServletRequest request);
}

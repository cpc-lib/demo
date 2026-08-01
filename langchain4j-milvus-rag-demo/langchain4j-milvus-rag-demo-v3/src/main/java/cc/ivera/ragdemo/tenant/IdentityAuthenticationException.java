package cc.ivera.ragdemo.tenant;

import cc.ivera.ragdemo.exception.MissingTenantContextException;

public class IdentityAuthenticationException extends MissingTenantContextException {

    public IdentityAuthenticationException(String message) {
        super(message);
    }

    public IdentityAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

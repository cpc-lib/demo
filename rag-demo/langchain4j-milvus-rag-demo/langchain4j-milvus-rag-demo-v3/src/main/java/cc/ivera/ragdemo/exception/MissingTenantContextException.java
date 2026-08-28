package cc.ivera.ragdemo.exception;

public class MissingTenantContextException extends RuntimeException {

    public MissingTenantContextException(String message) {
        super(message);
    }

    public MissingTenantContextException(String message, Throwable cause) {
        super(message, cause);
    }
}

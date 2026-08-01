package cc.ivera.ragdemo.exception;

/**
 * 资源耗尽异常
 * 用于表示系统资源（如内存、线程池、连接池等）已耗尽
 */
public class ResourceExhaustedException extends RuntimeException {

    private final String resourceType;
    private final long retryAfterSeconds;

    public ResourceExhaustedException(String message) {
        super(message);
        this.resourceType = null;
        this.retryAfterSeconds = 0;
    }

    public ResourceExhaustedException(String message, String resourceType) {
        super(message);
        this.resourceType = resourceType;
        this.retryAfterSeconds = 0;
    }

    public ResourceExhaustedException(String message, String resourceType, long retryAfterSeconds) {
        super(message);
        this.resourceType = resourceType;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ResourceExhaustedException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.retryAfterSeconds = 0;
    }

    public ResourceExhaustedException(String message, String resourceType, Throwable cause) {
        super(message, cause);
        this.resourceType = resourceType;
        this.retryAfterSeconds = 0;
    }

    public String getResourceType() {
        return resourceType;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
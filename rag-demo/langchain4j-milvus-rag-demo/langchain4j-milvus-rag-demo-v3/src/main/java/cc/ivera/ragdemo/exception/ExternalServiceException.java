package cc.ivera.ragdemo.exception;

/**
 * 外部服务调用异常
 * 用于封装调用外部服务（如LLM、Embedding、Milvus、Redis等）时发生的错误
 */
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final int httpStatusCode;

    public ExternalServiceException(String message) {
        super(message);
        this.serviceName = null;
        this.httpStatusCode = 0;
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = null;
        this.httpStatusCode = 0;
    }

    public ExternalServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.httpStatusCode = 0;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.httpStatusCode = 0;
    }

    public ExternalServiceException(String serviceName, String message, int httpStatusCode) {
        super(message);
        this.serviceName = serviceName;
        this.httpStatusCode = httpStatusCode;
    }

    public ExternalServiceException(String serviceName, String message, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.httpStatusCode = httpStatusCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
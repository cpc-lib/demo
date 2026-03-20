package cc.ivera.exception;

/**
 * 自定义运行类异常抛出
 */
public class ServiceRuntimeException extends RuntimeException{
    public ServiceRuntimeException() {
    }
    public ServiceRuntimeException(String msg) {
        super(msg);
    }
}

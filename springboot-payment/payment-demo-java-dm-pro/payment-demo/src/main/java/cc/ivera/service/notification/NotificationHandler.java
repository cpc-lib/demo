package cc.ivera.service.notification;

/**
 * 通知处理链节点 — Chain of Responsibility 模式。
 *
 * 每个 Handler 处理通知的一个切面（验签、幂等、业务校验、状态更新、记录）。
 */
public interface NotificationHandler {

    /**
     * 处理通知上下文。
     *
     * @param context 通知上下文
     * @param chain   责任链（用于调用下一个节点）
     */
    void handle(NotificationContext context, NotificationChain chain);

    /**
     * 返回处理器名称，用于日志和调试。
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}

package cc.ivera.service.command;

/**
 * 支付命令接口 — Command 模式。
 *
 * 将支付相关的操作（下单、通知处理、退款、关单等）封装为命令对象，
 * 由 PaymentCommandInvoker 统一执行（含分布式锁、事务模板）。
 *
 * @param <T> 命令执行结果类型
 */
public interface PaymentCommand<T> {

    /**
     * 执行命令。
     *
     * @return 命令执行结果
     */
    T execute();

    /**
     * 获取命令名称（用于日志）。
     */
    default String getCommandName() {
        return getClass().getSimpleName();
    }

    /**
     * 获取分布式锁 key（用于并发控制）。
     * 返回 null 表示不需要加锁。
     */
    default String getLockKey() {
        return null;
    }

    /**
     * 获取锁等待时间（毫秒）。
     */
    default long getLockWaitMs() {
        return 5000L;
    }

    /**
     * 获取锁租约时间（毫秒），-1 表示看门狗自动续期。
     */
    default long getLockLeaseMs() {
        return -1L;
    }

    /**
     * 是否需要事务。
     */
    default boolean requiresTransaction() {
        return true;
    }
}

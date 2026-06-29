package cc.ivera.service.command;

import cc.ivera.lock.DistributedLockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 支付命令调用器 — Command 模式。
 *
 * 统一封装分布式锁和事务模板的执行逻辑，避免在各 Service 中重复样板代码。
 */
@Component
@Slf4j
public class PaymentCommandInvoker {

    private final DistributedLockTemplate distributedLockTemplate;
    private final TransactionTemplate transactionTemplate;

    public PaymentCommandInvoker(DistributedLockTemplate distributedLockTemplate,
                                 TransactionTemplate transactionTemplate) {
        this.distributedLockTemplate = distributedLockTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 执行支付命令（自动加锁 + 事务）。
     *
     * @param command 支付命令
     * @param <T>     返回值类型
     * @return 命令执行结果
     */
    public <T> T invoke(PaymentCommand<T> command) {
        String lockKey = command.getLockKey();
        if (lockKey != null) {
            return distributedLockTemplate.execute(
                    lockKey,
                    command.getLockWaitMs(),
                    command.getLockLeaseMs(),
                    () -> executeWithTransaction(command));
        }
        return executeWithTransaction(command);
    }

    private <T> T executeWithTransaction(PaymentCommand<T> command) {
        if (command.requiresTransaction()) {
            return transactionTemplate.execute(status -> {
                log.info("执行支付命令: {}", command.getCommandName());
                return command.execute();
            });
        }
        log.info("执行支付命令(无事务): {}", command.getCommandName());
        return command.execute();
    }
}

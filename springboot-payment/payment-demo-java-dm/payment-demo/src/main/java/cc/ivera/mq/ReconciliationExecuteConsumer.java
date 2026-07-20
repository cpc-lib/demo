package cc.ivera.mq;

import cc.ivera.config.ReconciliationRabbitConfig;
import cc.ivera.service.reconciliation.ReconciliationBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class ReconciliationExecuteConsumer {

    private final ReconciliationBatchService reconciliationBatchService;

    public ReconciliationExecuteConsumer(ReconciliationBatchService reconciliationBatchService) {
        this.reconciliationBatchService = reconciliationBatchService;
    }

    @RabbitListener(queues = ReconciliationRabbitConfig.RECONCILIATION_EXECUTE_QUEUE)
    public void handleExecute(ReconciliationExecuteMessage message) {
        if (message == null || !StringUtils.hasText(message.getBatchNo())) {
            log.warn("收到空的对账执行消息，忽略处理");
            return;
        }

        String batchNo = message.getBatchNo();
        log.info("开始处理对账执行消息，batchNo={}", batchNo);
        try {
            reconciliationBatchService.executeBatch(batchNo);
            log.info("对账执行消息处理完成，batchNo={}", batchNo);
        } catch (Exception e) {
            log.error("对账执行消息处理失败，batchNo={}", batchNo, e);
            throw e;
        }
    }

    @RabbitListener(queues = ReconciliationRabbitConfig.RECONCILIATION_RETRY_RELEASE_QUEUE)
    public void handleRetry(ReconciliationExecuteMessage message) {
        if (message == null || !StringUtils.hasText(message.getBatchNo())) {
            log.warn("收到空的对账重试消息，忽略处理");
            return;
        }

        String batchNo = message.getBatchNo();
        Integer retryCount = message.getRetryCount() == null ? 0 : message.getRetryCount();
        log.info("开始处理对账重试消息，batchNo={}, retryCount={}", batchNo, retryCount);

        if (retryCount < 3) {
            try {
                reconciliationBatchService.executeBatch(batchNo);
                log.info("对账重试消息处理完成，batchNo={}, retryCount={}", batchNo, retryCount);
            } catch (Exception e) {
                log.error("对账重试消息处理失败，batchNo={}, retryCount={}", batchNo, retryCount, e);
                throw e;
            }
        } else {
            log.error("对账重试次数已达上限（3次），不再重试，batchNo={}", batchNo);
        }
    }
}

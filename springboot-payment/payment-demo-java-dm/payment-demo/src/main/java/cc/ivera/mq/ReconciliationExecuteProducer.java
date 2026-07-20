package cc.ivera.mq;

import cc.ivera.config.ReconciliationRabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class ReconciliationExecuteProducer {

    private final RabbitTemplate rabbitTemplate;

    public ReconciliationExecuteProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendExecute(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            throw new IllegalArgumentException("发送对账执行消息失败，batchNo 不能为空");
        }

        ReconciliationExecuteMessage message = new ReconciliationExecuteMessage();
        message.setBatchNo(batchNo);
        message.setRetryCount(0);

        rabbitTemplate.convertAndSend(
                ReconciliationRabbitConfig.RECONCILIATION_EXECUTE_EXCHANGE,
                ReconciliationRabbitConfig.RECONCILIATION_EXECUTE_ROUTING_KEY,
                message);
        log.info("对账执行消息已发送，batchNo={}", batchNo);
    }

    public void sendRetry(String batchNo, int retryCount) {
        if (!StringUtils.hasText(batchNo)) {
            throw new IllegalArgumentException("发送对账重试消息失败，batchNo 不能为空");
        }

        ReconciliationExecuteMessage message = new ReconciliationExecuteMessage();
        message.setBatchNo(batchNo);
        message.setRetryCount(retryCount);

        rabbitTemplate.convertAndSend(
                ReconciliationRabbitConfig.RECONCILIATION_EXECUTE_EXCHANGE,
                ReconciliationRabbitConfig.RECONCILIATION_RETRY_DELAY_ROUTING_KEY,
                message);
        log.info("对账重试消息已发送，batchNo={}, retryCount={}", batchNo, retryCount);
    }
}

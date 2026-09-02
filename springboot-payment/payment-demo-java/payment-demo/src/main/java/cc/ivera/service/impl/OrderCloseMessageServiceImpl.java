package cc.ivera.service.impl;

import cc.ivera.mq.OutboxEventTypes;
import cc.ivera.service.MessageOutboxService;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class OrderCloseMessageServiceImpl implements OrderCloseMessageService {

    private final MessageOutboxService messageOutboxService;

    public OrderCloseMessageServiceImpl(
            MessageOutboxService messageOutboxService
    ) {
        this.messageOutboxService = messageOutboxService;
    }

    @Override
    public void sendCloseOrderMessage(String orderNo, String paymentType) {
        if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(paymentType)) {
            throw new IllegalArgumentException("发送延迟关单消息失败，订单号或支付类型为空");
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("orderNo", orderNo);
        payload.put("paymentType", paymentType);
        messageOutboxService.insertOnce(
                OutboxEventTypes.ORDER_CLOSE_SCHEDULED + ":" + orderNo,
                "ORDER",
                orderNo,
                OutboxEventTypes.ORDER_CLOSE_SCHEDULED,
                JsonUtils.toJson(payload)
        );
        log.info("延迟关单事件已写入Outbox，orderNo={}, paymentType={}", orderNo, paymentType);
    }
}

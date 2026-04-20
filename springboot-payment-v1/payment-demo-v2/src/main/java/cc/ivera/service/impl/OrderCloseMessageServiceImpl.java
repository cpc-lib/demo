package cc.ivera.service.impl;

import cc.ivera.config.OrderCloseRabbitConfig;
import cc.ivera.mq.OrderCloseMessage;
import cc.ivera.service.OrderCloseMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Service
@Slf4j
public class OrderCloseMessageServiceImpl implements OrderCloseMessageService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void sendCloseOrderMessage(String orderNo, String paymentType) {
        if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(paymentType)) {
            throw new IllegalArgumentException("发送延迟关单消息失败，订单号或支付类型为空");
        }

        OrderCloseMessage message = new OrderCloseMessage();
        message.setOrderNo(orderNo);
        message.setPaymentType(paymentType);

        rabbitTemplate.convertAndSend(
                OrderCloseRabbitConfig.ORDER_CLOSE_EVENT_EXCHANGE,
                OrderCloseRabbitConfig.ORDER_CLOSE_DELAY_ROUTING_KEY,
                message);
        log.info("延迟关单消息已发送，orderNo={}, paymentType={}", orderNo, paymentType);
    }
}

package cc.ivera.ordermachine.service.action;

import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.enums.BusinessType;
import cc.ivera.ordermachine.domain.enums.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaySuccessAction implements StateAction {

    @Override
    public boolean supports(String businessType, OrderEvent event) {
        return BusinessType.NORMAL.name().equals(businessType) && OrderEvent.PAY == event;
    }

    @Override
    public void execute(Order order, OrderEvent event, String fromStatus, String toStatus) {
        log.info("支付后置动作: orderNo={}, fromStatus={}, toStatus={}", order.getOrderNo(), fromStatus, toStatus);
    }
}

package cc.ivera.ordermachine.service.guard;

import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.enums.BusinessType;
import cc.ivera.ordermachine.domain.enums.OrderEvent;
import cc.ivera.ordermachine.domain.enums.OrderStatus;
import cc.ivera.ordermachine.exception.BizException;
import org.springframework.stereotype.Component;

@Component
public class ConfirmReceiptGuard implements StateGuard {

    @Override
    public boolean supports(String businessType, OrderEvent event) {
        return BusinessType.NORMAL.name().equals(businessType) && OrderEvent.CONFIRM_RECEIPT == event;
    }

    @Override
    public void check(Order order, OrderEvent event) {
        if (!OrderStatus.SHIPPED.name().equals(order.getStatus())) {
            throw new BizException("只有已发货订单才能确认收货");
        }
    }
}

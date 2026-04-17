package cc.ivera.ordermachine.service.guard;

import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.enums.BusinessType;
import cc.ivera.ordermachine.domain.enums.OrderEvent;
import cc.ivera.ordermachine.domain.enums.OrderStatus;
import cc.ivera.ordermachine.exception.BizException;
import org.springframework.stereotype.Component;

@Component
public class ApplyRefundGuard implements StateGuard {

    @Override
    public boolean supports(String businessType, OrderEvent event) {
        return BusinessType.NORMAL.name().equals(businessType) && OrderEvent.APPLY_REFUND == event;
    }

    @Override
    public void check(Order order, OrderEvent event) {
        boolean allowed = OrderStatus.PAID.name().equals(order.getStatus())
                || OrderStatus.SHIPPED.name().equals(order.getStatus());
        if (!allowed) {
            throw new BizException("当前状态不允许申请退款");
        }
    }
}

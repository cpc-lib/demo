package cc.ivera.ordermachine.service.guard;

import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.enums.BusinessType;
import cc.ivera.ordermachine.domain.enums.OrderEvent;
import cc.ivera.ordermachine.exception.BizException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PayGuard implements StateGuard {

    @Override
    public boolean supports(String businessType, OrderEvent event) {
        return BusinessType.NORMAL.name().equals(businessType) && OrderEvent.PAY == event;
    }

    @Override
    public void check(Order order, OrderEvent event) {
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("订单金额必须大于0，不能支付");
        }
    }
}

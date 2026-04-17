package cc.ivera.ordermachine.service.guard;

import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.enums.OrderEvent;

public interface StateGuard {

    boolean supports(String businessType, OrderEvent event);

    void check(Order order, OrderEvent event);
}

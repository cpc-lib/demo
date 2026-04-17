package cc.ivera.ordermachine.service.action;

import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.enums.OrderEvent;

public interface StateAction {

    boolean supports(String businessType, OrderEvent event);

    void execute(Order order, OrderEvent event, String fromStatus, String toStatus);
}

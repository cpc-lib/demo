package cc.ivera.ordermachine.service;

import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.entity.OrderStateTransition;
import cc.ivera.ordermachine.domain.enums.OrderEvent;
import cc.ivera.ordermachine.domain.enums.RollbackMode;
import cc.ivera.ordermachine.exception.BizException;
import cc.ivera.ordermachine.mapper.OrderMapper;
import cc.ivera.ordermachine.service.action.StateAction;
import cc.ivera.ordermachine.service.guard.StateGuard;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderStateMachineService {

    private final OrderMapper orderMapper;
    private final OrderStateTransitionService transitionService;
    private final OrderStateLogService stateLogService;
    private final List<StateGuard> guardList;
    private final List<StateAction> actionList;

    private List<StateGuard> guards = new ArrayList<>();
    private List<StateAction> actions = new ArrayList<>();

    @PostConstruct
    public void init() {
        this.guards = guardList;
        this.actions = actionList;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order transit(Long orderId, OrderEvent event, String operator, String remark) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }

        String businessType = order.getBusinessType();
        String fromStatus = order.getStatus();

        OrderStateTransition transition = transitionService.getTransition(businessType, fromStatus, event.name());
        if (transition == null) {
            throw new BizException(String.format(
                    "不允许状态流转: businessType=%s, currentStatus=%s, event=%s",
                    businessType, fromStatus, event.name()));
        }

        for (StateGuard guard : guards) {
            if (guard.supports(businessType, event)) {
                guard.check(order, event);
            }
        }

        if (OrderEvent.APPLY_REFUND == event) {
            order.setBeforeRefundStatus(fromStatus);
        }

        String toStatus = calculateNextStatus(order, transition);
        order.setStatus(toStatus);

        if (OrderEvent.REFUND_SUCCESS == event || OrderEvent.REFUND_REJECT == event) {
            order.setBeforeRefundStatus(null);
        }

        int updatedRows = orderMapper.updateById(order);
        if (updatedRows != 1) {
            throw new BizException("订单状态更新失败，请重试");
        }

        stateLogService.save(order, fromStatus, event.name(), toStatus, operator, remark);

        for (StateAction action : actions) {
            if (action.supports(businessType, event)) {
                action.execute(order, event, fromStatus, toStatus);
            }
        }

        return order;
    }

    private String calculateNextStatus(Order order, OrderStateTransition transition) {
        String rollbackMode = transition.getRollbackMode();
        if (RollbackMode.PREVIOUS_STATUS.name().equalsIgnoreCase(rollbackMode)) {
            String previousStatus = order.getBeforeRefundStatus();
            if (previousStatus == null || previousStatus.isBlank()) {
                throw new BizException("缺少退款前状态，无法回退");
            }
            return previousStatus;
        }
        return transition.getNextStatus();
    }
}

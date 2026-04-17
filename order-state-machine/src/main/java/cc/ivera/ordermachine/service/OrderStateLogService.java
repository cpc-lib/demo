package cc.ivera.ordermachine.service;

import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.entity.OrderStateLog;
import cc.ivera.ordermachine.mapper.OrderStateLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderStateLogService {

    private final OrderStateLogMapper orderStateLogMapper;

    public void save(Order order, String fromStatus, String event, String toStatus, String operator, String remark) {
        OrderStateLog stateLog = new OrderStateLog();
        stateLog.setOrderId(order.getId());
        stateLog.setOrderNo(order.getOrderNo());
        stateLog.setBusinessType(order.getBusinessType());
        stateLog.setFromStatus(fromStatus);
        stateLog.setEvent(event);
        stateLog.setToStatus(toStatus);
        stateLog.setOperator(operator);
        stateLog.setRemark(remark);
        stateLog.setCreateTime(LocalDateTime.now());
        orderStateLogMapper.insert(stateLog);
    }
}

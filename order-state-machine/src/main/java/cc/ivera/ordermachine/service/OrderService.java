package cc.ivera.ordermachine.service;

import cc.ivera.ordermachine.components.OrderNoGenerateHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.ordermachine.domain.dto.CreateOrderRequest;
import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.domain.enums.BusinessType;
import cc.ivera.ordermachine.domain.enums.OrderEvent;
import cc.ivera.ordermachine.domain.enums.OrderStatus;
import cc.ivera.ordermachine.exception.BizException;
import cc.ivera.ordermachine.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderStateMachineService orderStateMachineService;
    private final OrderNoGenerateHelper orderNoGenerateHelper;


    @Transactional(rollbackFor = Exception.class)
    public Order create(CreateOrderRequest request) {
        validateBusinessType(request.getBusinessType());
        Order order = new Order();
        order.setOrderNo(orderNoGenerateHelper.nextOrderNo());
        order.setBusinessType(request.getBusinessType());
        order.setStatus(OrderStatus.CREATED.name());
        order.setAmount(request.getAmount());
        order.setVersion(0);
        order.setRemark(request.getRemark());
        orderMapper.insert(order);
        return order;
    }

    public Order getById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    public List<Order> list() {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getId));
    }

    public Order transit(Long orderId, String event, String operator, String remark) {
        OrderEvent orderEvent;
        try {
            orderEvent = OrderEvent.valueOf(event);
        } catch (IllegalArgumentException ex) {
            throw new BizException("不支持的事件: " + event);
        }
        return orderStateMachineService.transit(orderId, orderEvent, operator, remark);
    }

    private void validateBusinessType(String businessType) {
        try {
            BusinessType.valueOf(businessType);
        } catch (IllegalArgumentException ex) {
            throw new BizException("不支持的业务类型: " + businessType);
        }
    }
}

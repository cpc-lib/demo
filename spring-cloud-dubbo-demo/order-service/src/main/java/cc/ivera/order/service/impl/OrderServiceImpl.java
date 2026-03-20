package cc.ivera.order.service.impl;

import cc.ivera.dubbo.domain.Order;
import cc.ivera.order.mapper.OrderMapper;
import cc.ivera.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    public Order queryOrderById(Long orderId) {
        // 1.查询订单
        return orderMapper.findById(orderId);
    }
}

package cc.ivera.order.service;


import cc.ivera.dubbo.domain.Order;

public interface OrderService {

    Order queryOrderById(Long orderId);
}

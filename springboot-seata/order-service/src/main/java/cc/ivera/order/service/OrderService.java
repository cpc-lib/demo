package cc.ivera.order.service;

import cc.ivera.order.entity.Order;

public interface OrderService {

    /**
     * 创建订单
     */
    Long create(Order order);
}
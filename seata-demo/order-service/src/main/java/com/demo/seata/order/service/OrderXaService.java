package com.demo.seata.order.service;

import com.demo.seata.common.domain.dto.OrderCreateRequest;
import com.demo.seata.order.entity.OrderInfo;

/**
 * XA 模式订单业务接口
 */
public interface OrderXaService {

    /**
     * XA 模式创建订单
     *
     * @param request 订单请求
     * @return 订单详情
     */
    OrderInfo createOrder(OrderCreateRequest request);
}

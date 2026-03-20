package com.example.orderdemo.service;

import com.example.orderdemo.domain.dto.CreateOrderRequest;
import com.example.orderdemo.domain.dto.UpdateOrderStatusRequest;
import com.example.orderdemo.domain.vo.OrderDetailVO;
import com.example.orderdemo.domain.vo.PageResult;

public interface OrderService {

  Long createOrder(CreateOrderRequest req);

  OrderDetailVO getOrderDetail(Long orderId);

  PageResult<OrderDetailVO> pageOrdersByUser(Long userId, long page, long size);

  void updateOrderStatus(Long orderId, UpdateOrderStatusRequest req);
}

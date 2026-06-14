package cc.ivera.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cc.ivera.mapper.OrderMapper;
import cc.ivera.model.Order;

@Service
public class OrderService {
	
	@Autowired
	private OrderMapper orderMapper;
	
	public List<Order> getOrderListByUserId(Integer userId) {
		return orderMapper.getOrderListByUserId(userId);
	}
	
	public void createOrder(Order order) {
		orderMapper.createOrder(order);
	}

}

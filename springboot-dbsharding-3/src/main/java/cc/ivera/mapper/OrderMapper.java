package cc.ivera.mapper;

import java.util.List;

import cc.ivera.model.Order;

public interface OrderMapper {
	
	List<Order> getOrderListByUserId(Integer userId);
	
	void createOrder(Order order);

}

package cc.ivera.dao;


import cc.ivera.entity.Order;

import java.util.List;

/**
 * @version v1.0
 * @description
 * @since 2020/2/9 14:16
 */
public interface OrderDao {

    int insertSelective(Order record);

    int updateSelective(Order record);

    Order selectOneByOrderId(String orderId);

    List<Order> selectByUserId(Long userId);
}

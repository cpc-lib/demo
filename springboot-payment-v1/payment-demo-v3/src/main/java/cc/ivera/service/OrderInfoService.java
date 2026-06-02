package cc.ivera.service;

import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfo createOrReuseOrder(Long productId, String paymentType);

    void saveCodeUrl(String orderNo, String codeUrl);

    List<OrderInfo> listOrderByCreateTimeDesc();

    void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus);

    boolean updateStatusByOrderNoIfStatus(String orderNo, OrderStatus currentStatus, OrderStatus targetStatus);

    String getOrderStatus(String orderNo);

    OrderInfo getOrderByOrderNo(String orderNo);

    OrderInfo getOrderByOrderNoForUpdate(String orderNo);
}

package cc.ivera.service;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;

import java.util.List;

public interface InventoryService {

    void reserve(OrderInfo order, List<OrderItem> items);

    boolean commitPayment(String orderNo);

    boolean releaseReservation(String orderNo);
}

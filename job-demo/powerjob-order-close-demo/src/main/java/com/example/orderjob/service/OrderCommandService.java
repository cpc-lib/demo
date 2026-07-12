package com.example.orderjob.service;

import com.example.orderjob.domain.OrderSnapshot;
import com.example.orderjob.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCommandService {

    private final OrderRepository orderRepository;

    public OrderCommandService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderSnapshot pay(Long orderId) {
        int affectedRows = orderRepository.markPaid(orderId);
        OrderSnapshot current = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (affectedRows == 0) {
            throw new IllegalStateException("Order cannot be paid, current status=" + current.status());
        }
        return current;
    }
}

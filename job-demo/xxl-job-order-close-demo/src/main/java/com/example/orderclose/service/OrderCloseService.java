package com.example.orderclose.service;

import com.example.orderclose.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderCloseService {

    private final OrderRepository orderRepository;

    public OrderCloseService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderCloseResult closeTimeoutOrders(int batchSize, int maxRounds) {
        validate(batchSize, maxRounds);

        int scanned = 0;
        int closed = 0;
        int rounds = 0;
        long lastId = 0L;
        boolean reachedLimit = false;

        while (rounds < maxRounds) {
            List<Long> orderIds = orderRepository.findExpiredUnpaidOrderIds(lastId, batchSize);
            if (orderIds.isEmpty()) {
                break;
            }

            rounds++;
            scanned += orderIds.size();

            for (Long orderId : orderIds) {
                closed += orderRepository.closeIfStillExpiredAndUnpaid(orderId);
            }

            lastId = orderIds.get(orderIds.size() - 1);

            if (orderIds.size() < batchSize) {
                break;
            }

            if (rounds == maxRounds) {
                reachedLimit = true;
            }
        }

        return new OrderCloseResult(scanned, closed, scanned - closed, rounds, reachedLimit);
    }

    private static void validate(int batchSize, int maxRounds) {
        if (batchSize < 1 || batchSize > 5000) {
            throw new IllegalArgumentException("batchSize must be between 1 and 5000");
        }
        if (maxRounds < 1 || maxRounds > 1000) {
            throw new IllegalArgumentException("maxRounds must be between 1 and 1000");
        }
    }
}

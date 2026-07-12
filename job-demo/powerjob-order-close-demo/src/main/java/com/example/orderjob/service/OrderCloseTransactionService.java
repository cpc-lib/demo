package com.example.orderjob.service;

import com.example.orderjob.domain.CloseOutcome;
import com.example.orderjob.domain.CloseResult;
import com.example.orderjob.domain.OrderSnapshot;
import com.example.orderjob.domain.OrderStatus;
import com.example.orderjob.repository.OrderCloseLogRepository;
import com.example.orderjob.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrderCloseTransactionService {

    private static final String CLOSE_REASON = "PAY_TIMEOUT";

    private final OrderRepository orderRepository;
    private final OrderCloseLogRepository closeLogRepository;

    public OrderCloseTransactionService(OrderRepository orderRepository,
                                        OrderCloseLogRepository closeLogRepository) {
        this.orderRepository = orderRepository;
        this.closeLogRepository = closeLogRepository;
    }

    /**
     * 每个订单独立事务。批处理中某一条失败不会回滚此前已成功关闭的订单，PowerJob 重试仍然安全。
     */
    @Transactional
    public CloseResult closeOne(Long orderId, String triggerSource, String schedulerInstanceId) {
        int affectedRows = orderRepository.closeExpiredUnpaid(orderId, CLOSE_REASON);
        if (affectedRows == 1) {
            OrderSnapshot closedOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Order disappeared after close: " + orderId));
            closeLogRepository.insertOnce(
                    closedOrder.id(),
                    closedOrder.orderNo(),
                    triggerSource,
                    schedulerInstanceId
            );
            return new CloseResult(orderId, closedOrder.orderNo(), CloseOutcome.CLOSED);
        }

        Optional<OrderSnapshot> currentOptional = orderRepository.findById(orderId);
        if (currentOptional.isEmpty()) {
            return new CloseResult(orderId, null, CloseOutcome.NOT_FOUND);
        }

        OrderSnapshot current = currentOptional.get();
        if (current.status() != OrderStatus.UNPAID) {
            return new CloseResult(orderId, current.orderNo(), CloseOutcome.ALREADY_HANDLED);
        }
        if (current.expireTime().isAfter(LocalDateTime.now())) {
            return new CloseResult(orderId, current.orderNo(), CloseOutcome.NOT_EXPIRED);
        }

        // 极短暂并发窗口下保守按已处理返回；下一轮扫描会再次校验。
        return new CloseResult(orderId, current.orderNo(), CloseOutcome.ALREADY_HANDLED);
    }
}

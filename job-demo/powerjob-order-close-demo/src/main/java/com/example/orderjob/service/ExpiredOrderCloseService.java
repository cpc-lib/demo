package com.example.orderjob.service;

import com.example.orderjob.domain.CloseOutcome;
import com.example.orderjob.domain.CloseResult;
import com.example.orderjob.domain.OrderCloseSummary;
import com.example.orderjob.job.CloseJobParam;
import com.example.orderjob.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpiredOrderCloseService {

    private static final Logger log = LoggerFactory.getLogger(ExpiredOrderCloseService.class);

    private final OrderRepository orderRepository;
    private final OrderCloseTransactionService transactionService;

    public ExpiredOrderCloseService(OrderRepository orderRepository,
                                    OrderCloseTransactionService transactionService) {
        this.orderRepository = orderRepository;
        this.transactionService = transactionService;
    }

    public OrderCloseSummary execute(CloseJobParam param,
                                     String triggerSource,
                                     String schedulerInstanceId) {
        param.validate();

        int scanned = 0;
        int closed = 0;
        int alreadyHandled = 0;
        int notExpired = 0;
        int notFound = 0;
        int pages = 0;
        long lastId = 0L;
        boolean truncated = false;

        while (pages < param.getMaxPages()) {
            List<Long> orderIds = orderRepository.findExpiredUnpaidIds(lastId, param.getBatchSize());
            if (orderIds.isEmpty()) {
                break;
            }

            pages++;
            for (Long orderId : orderIds) {
                lastId = orderId;
                scanned++;
                CloseResult result = transactionService.closeOne(orderId, triggerSource, schedulerInstanceId);
                if (result.outcome() == CloseOutcome.CLOSED) {
                    closed++;
                } else if (result.outcome() == CloseOutcome.ALREADY_HANDLED) {
                    alreadyHandled++;
                } else if (result.outcome() == CloseOutcome.NOT_EXPIRED) {
                    notExpired++;
                } else if (result.outcome() == CloseOutcome.NOT_FOUND) {
                    notFound++;
                }
            }

            if (orderIds.size() < param.getBatchSize()) {
                break;
            }
        }

        if (pages >= param.getMaxPages()
                && !orderRepository.findExpiredUnpaidIds(lastId, 1).isEmpty()) {
            truncated = true;
        }

        OrderCloseSummary summary = new OrderCloseSummary(
                scanned, closed, alreadyHandled, notExpired, notFound, pages, truncated
        );
        log.info("Expired order close finished: {}", summary.toMessage());
        return summary;
    }
}

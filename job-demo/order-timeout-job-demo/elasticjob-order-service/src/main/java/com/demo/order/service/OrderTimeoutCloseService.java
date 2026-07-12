package com.demo.order.service;

import com.demo.order.domain.OrderCloseJobLog;
import com.demo.order.domain.OrderEntity;
import com.demo.order.mapper.OrderCloseJobLogMapper;
import com.demo.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderTimeoutCloseService {
    private final OrderMapper orderMapper;
    private final OrderCloseJobLogMapper logMapper;

    @Value("${order.job.batch-size:100}")
    private int batchSize;

    @Transactional(rollbackFor = Exception.class)
    public int closeTimeoutUnpaidOrders(String jobType) {
        String batchNo = UUID.randomUUID().toString().replace("-", "");
        OrderCloseJobLog log = new OrderCloseJobLog();
        log.setJobType(jobType);
        log.setInstanceId(instanceId());
        log.setBatchNo(batchNo);
        log.setStatus("RUNNING");
        log.setScannedCount(0);
        log.setClosedCount(0);
        log.setStartedAt(LocalDateTime.now());
        logMapper.insert(log);
        try {
            List<OrderEntity> timeoutOrders = orderMapper.selectTimeoutUnpaidOrders(batchSize);
            int closed = 0;
            for (OrderEntity order : timeoutOrders) {
                closed += orderMapper.closeByOptimisticLock(order.getId(), order.getVersion(), "TIMEOUT_AUTO_CLOSE_BY_" + jobType);
            }
            log.setScannedCount(timeoutOrders.size());
            log.setClosedCount(closed);
            log.setStatus("SUCCESS");
            log.setFinishedAt(LocalDateTime.now());
            logMapper.updateById(log);
            return closed;
        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMsg(e.getMessage());
            log.setFinishedAt(LocalDateTime.now());
            logMapper.updateById(log);
            throw e;
        }
    }

    private String instanceId() {
        try { return InetAddress.getLocalHost().getHostName(); } catch (Exception e) { return "unknown"; }
    }
}

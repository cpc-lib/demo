package com.example.orderjob.web;

import com.example.orderjob.domain.OrderCloseSummary;
import com.example.orderjob.domain.OrderSnapshot;
import com.example.orderjob.job.CloseJobParam;
import com.example.orderjob.repository.OrderRepository;
import com.example.orderjob.service.ExpiredOrderCloseService;
import com.example.orderjob.service.OrderCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderDemoController {

    private final OrderRepository orderRepository;
    private final OrderCommandService orderCommandService;
    private final ExpiredOrderCloseService closeService;

    public OrderDemoController(OrderRepository orderRepository,
                               OrderCommandService orderCommandService,
                               ExpiredOrderCloseService closeService) {
        this.orderRepository = orderRepository;
        this.orderCommandService = orderCommandService;
        this.closeService = closeService;
    }

    @PostMapping("/demo")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderSnapshot createDemoOrder(
            @RequestParam(defaultValue = "99.90") BigDecimal amount,
            @RequestParam(defaultValue = "true") boolean expired) {
        LocalDateTime expireTime = expired
                ? LocalDateTime.now().minusMinutes(5)
                : LocalDateTime.now().plusMinutes(30);
        long orderId = orderRepository.createDemoOrder(amount, expireTime);
        return orderRepository.findById(orderId).orElseThrow();
    }

    @PostMapping("/{orderId}/pay")
    public OrderSnapshot pay(@PathVariable Long orderId) {
        try {
            return orderCommandService.pay(orderId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @GetMapping
    public List<OrderSnapshot> list(@RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return orderRepository.findLatest(safeLimit);
    }

    /** 仅用于本地联调；生产环境建议移除或加内部鉴权。 */
    @PostMapping("/internal/close-once")
    public Map<String, Object> closeOnce(
            @RequestParam(defaultValue = "200") int batchSize,
            @RequestParam(defaultValue = "50") int maxPages) {
        CloseJobParam param = new CloseJobParam();
        param.setBatchSize(batchSize);
        param.setMaxPages(maxPages);
        OrderCloseSummary summary = closeService.execute(param, "MANUAL_API", "manual");
        return Map.of("success", true, "summary", summary);
    }
}

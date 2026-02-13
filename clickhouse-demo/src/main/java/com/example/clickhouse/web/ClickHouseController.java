package com.example.clickhouse.web;

import com.example.clickhouse.repo.OrderRepository;
import com.example.clickhouse.web.dto.OrderCreateRequest;
import com.example.clickhouse.web.dto.OrderResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ClickHouseController {

    private final OrderRepository orderRepository;

    public ClickHouseController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/ch/ping")
    public String ping() {
        return orderRepository.ping();
    }

    @GetMapping("/orders/count")
    public long count() {
        return orderRepository.countOrders();
    }

    @GetMapping("/orders")
    public List<OrderResponse> list(@RequestParam(defaultValue = "100") int limit) {
        return orderRepository.listOrders(limit);
    }

    @PostMapping("/orders")
    public String create(@RequestBody OrderCreateRequest req) {
        orderRepository.insertOrder(req);
        return "OK";
    }
}

package com.example.orderclose.web;

import com.example.orderclose.repository.OrderRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private final JdbcTemplate jdbcTemplate;
    private final OrderRepository orderRepository;

    public OrderQueryController(JdbcTemplate jdbcTemplate, OrderRepository orderRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList("""
                SELECT id, order_no, status, amount, created_at, expire_time,
                       pay_time, close_time, close_reason, version, updated_at
                FROM biz_order
                ORDER BY id
                """);
    }

    @GetMapping("/expired-count")
    public Map<String, Integer> expiredCount() {
        return Map.of("expiredUnpaidCount", orderRepository.countExpiredUnpaidOrders());
    }
}

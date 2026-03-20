package com.example.order.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RestTemplate restTemplate;

    public OrderController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 简单模拟：根据订单ID查询订单，同时调用 user-service 获取用户信息。
     */
    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@PathVariable("id") Long id) {
        log.info("开始处理订单查询，orderId={}", id);

        // 调用 user-service
        String userUrl = "http://localhost:8082/users/" + id;
        @SuppressWarnings("unchecked")
        Map<String, Object> user = restTemplate.getForObject(userUrl, Map.class);

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", id);
        order.put("amount", 199.99);
        order.put("status", "CREATED");

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("user", user);

        log.info("订单查询完成，orderId={}", id);
        return result;
    }
}

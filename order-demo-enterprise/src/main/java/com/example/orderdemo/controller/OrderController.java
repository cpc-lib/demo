package com.example.orderdemo.controller;

import com.example.orderdemo.domain.dto.*;
import com.example.orderdemo.service.OrderCommandService;
import com.example.orderdemo.service.OrderDetailService;
import com.example.orderdemo.service.OrderSearchService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderCommandService orderCommandService;
    private final OrderSearchService orderSearchService;
    private final OrderDetailService orderDetailService;

    public OrderController(OrderCommandService orderCommandService,
                           OrderSearchService orderSearchService,
                           OrderDetailService orderDetailService) {
        this.orderCommandService = orderCommandService;
        this.orderSearchService = orderSearchService;
        this.orderDetailService = orderDetailService;
    }

    @PostMapping
    public CreateOrderResponse create(@RequestBody @Valid CreateOrderRequest req) {
        long orderId = orderCommandService.createOrder(req);
        return new CreateOrderResponse(orderId);
    }

    /**
     * 订单列表：严格走 ES
     */
    @GetMapping("/search")
    public OrderSearchResponse search(@Valid OrderSearchRequest req) {
        return orderSearchService.search(req);
    }

    /**
     * 订单详情：Redis 版本缓存（miss 回源 MySQL）
     */
    @GetMapping("/{orderId}")
    public OrderDetailDTO detail(@PathVariable long orderId) {
        return orderDetailService.getOrderDetail(orderId);
    }

    /**
     * 更新状态：DB + outbox；同时 bump version，避免缓存脏读
     */
    @PutMapping("/{orderId}/status")
    public void updateStatus(@PathVariable long orderId,
                             @RequestParam("userId") long userId,
                             @RequestBody @Valid UpdateOrderStatusRequest req) {
        orderCommandService.updateStatus(orderId, userId, req.getStatus());
        orderDetailService.bumpVersion(orderId);
    }
}

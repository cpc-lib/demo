package com.example.orderdemo.controller;

import com.example.orderdemo.common.Result;
import com.example.orderdemo.domain.dto.CreateOrderRequest;
import com.example.orderdemo.domain.dto.UpdateOrderStatusRequest;
import com.example.orderdemo.domain.vo.OrderDetailVO;
import com.example.orderdemo.domain.vo.PageResult;
import com.example.orderdemo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/orders/v1")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  public Result<Map<String, Object>> create(@Valid @RequestBody CreateOrderRequest req) {
    Long orderId = orderService.createOrder(req);
    return Result.ok(Map.of("orderId", orderId));
  }

  @GetMapping("/{orderId}")
  public Result<OrderDetailVO> detail(@PathVariable Long orderId) {
    return Result.ok(orderService.getOrderDetail(orderId));
  }

  @GetMapping("/by-user/{userId}")
  public Result<PageResult<OrderDetailVO>> pageByUser(@PathVariable Long userId,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size) {
    return Result.ok(orderService.pageOrdersByUser(userId, page, size));
  }

  @PutMapping("/{orderId}/status")
  public Result<Void> updateStatus(@PathVariable Long orderId, @Valid @RequestBody UpdateOrderStatusRequest req) {
    orderService.updateOrderStatus(orderId, req);
    return Result.ok(null);
  }
}

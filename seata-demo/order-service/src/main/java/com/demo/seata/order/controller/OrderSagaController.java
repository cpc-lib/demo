package com.demo.seata.order.controller;

import com.demo.seata.common.domain.dto.OrderCreateRequest;
import com.demo.seata.common.vo.Result;
import com.demo.seata.order.entity.OrderInfo;
import com.demo.seata.order.service.OrderSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SAGA 模式测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/order/saga")
@RequiredArgsConstructor
public class OrderSagaController {

    private final OrderSagaService orderSagaService;

    @PostMapping("/create")
    public Result<OrderInfo> createOrder(@RequestBody OrderCreateRequest request) {
        log.info("[HTTP请求 - SAGA模式下单] 收到请求: {}", request);
        OrderInfo order = orderSagaService.createOrder(request);
        return Result.success(order);
    }
}

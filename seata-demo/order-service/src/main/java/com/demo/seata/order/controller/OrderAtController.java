package com.demo.seata.order.controller;

import com.demo.seata.common.domain.dto.OrderCreateRequest;
import com.demo.seata.common.vo.Result;
import com.demo.seata.order.entity.OrderInfo;
import com.demo.seata.order.service.OrderAtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AT 模式测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/order/at")
@RequiredArgsConstructor
public class OrderAtController {

    private final OrderAtService orderAtService;

    @PostMapping("/create")
    public Result<OrderInfo> createOrder(@RequestBody OrderCreateRequest request) {
        log.info("[HTTP请求 - AT模式下单] 收到请求: {}", request);
        OrderInfo order = orderAtService.createOrder(request);
        return Result.success(order);
    }
}

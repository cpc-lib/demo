package com.demo.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.order.domain.OrderEntity;
import com.demo.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderMapper orderMapper;

    @GetMapping
    public List<OrderEntity> list() {
        return orderMapper.selectList(new LambdaQueryWrapper<OrderEntity>().orderByDesc(OrderEntity::getId).last("limit 20"));
    }
}

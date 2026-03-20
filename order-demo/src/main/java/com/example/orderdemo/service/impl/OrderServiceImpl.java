package com.example.orderdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.orderdemo.cache.OrderDetailCacheService;
import com.example.orderdemo.common.ApiException;
import com.example.orderdemo.domain.dto.CreateOrderRequest;
import com.example.orderdemo.domain.dto.UpdateOrderStatusRequest;
import com.example.orderdemo.domain.entity.Order;
import com.example.orderdemo.domain.entity.OrderItem;
import com.example.orderdemo.domain.enums.OrderStatus;
import com.example.orderdemo.domain.vo.OrderDetailVO;
import com.example.orderdemo.domain.vo.PageResult;
import com.example.orderdemo.mapper.OrderItemMapper;
import com.example.orderdemo.mapper.OrderMapper;
import com.example.orderdemo.no.OrderNoGenerator;
import com.example.orderdemo.service.OrderService;
import com.example.orderdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderMapper orderMapper;
  private final OrderItemMapper orderItemMapper;
  private final UserService userService;
  private final OrderNoGenerator orderNoGenerator;

  private final OrderDetailCacheService orderDetailCacheService;

  @Override
  @Transactional
  public Long createOrder(CreateOrderRequest req) {
    userService.getByIdOrThrow(req.getUserId());

    BigDecimal total = BigDecimal.ZERO;
    List<OrderItem> items = new ArrayList<>(req.getItems().size());
    for (CreateOrderRequest.Item it : req.getItems()) {
      BigDecimal line = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
      total = total.add(line);

      OrderItem oi = new OrderItem();
      oi.setProductId(it.getProductId());
      oi.setProductName(it.getProductName());
      oi.setUnitPrice(it.getUnitPrice());
      oi.setQuantity(it.getQuantity());
      oi.setLineAmount(line);
      items.add(oi);
    }

    //String orderNo = "OD" + System.currentTimeMillis() + "-" + (int) (Math.random() * 9000 + 1000);
    String orderNo = orderNoGenerator.nextOrderNo();

    Order o = new Order();
    o.setUserId(req.getUserId());
    o.setOrderNo(orderNo);
    o.setTotalAmount(total);
    o.setStatus(OrderStatus.CREATED.name());
    o.setRemark(req.getRemark());
    orderMapper.insert(o);

    for (OrderItem oi : items) {
      oi.setOrderId(o.getId());
      orderItemMapper.insert(oi);
    }

    // 事务提交后：bump 版本，让后续读到最新（旧缓存自然失效）
    afterCommit(() -> orderDetailCacheService.bumpVersion(o.getId()));

    return o.getId();
  }

  @Override
  public OrderDetailVO getOrderDetail(Long orderId) {
    // 读走版本化缓存
    return orderDetailCacheService.getOrLoad(orderId, OrderDetailVO.class, () -> {
      Order o = orderMapper.selectById(orderId);
      if (o == null) {
        throw ApiException.notFound("订单不存在: " + orderId);
      }
      List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
          .eq(OrderItem::getOrderId, orderId)
          .orderByAsc(OrderItem::getId));
      return toDetailVO(o, items);
    });
  }

  @Override
  public PageResult<OrderDetailVO> pageOrdersByUser(Long userId, long page, long size) {
    userService.getByIdOrThrow(userId);

    Page<Order> p = new Page<>(page, size);
    Page<Order> res = orderMapper.selectPage(p, new LambdaQueryWrapper<Order>()
        .eq(Order::getUserId, userId)
        .orderByDesc(Order::getCreatedAt)
        .orderByDesc(Order::getId));

    List<Order> orders = res.getRecords();
    if (orders.isEmpty()) {
      return new PageResult<>(res.getTotal(), page, size, Collections.emptyList());
    }

    List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
    List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
        .in(OrderItem::getOrderId, orderIds)
        .orderByAsc(OrderItem::getId));

    Map<Long, List<OrderItem>> itemsByOrderId = items.stream()
        .collect(Collectors.groupingBy(OrderItem::getOrderId));

    List<OrderDetailVO> records = orders.stream()
        .map(o -> toDetailVO(o, itemsByOrderId.getOrDefault(o.getId(), Collections.emptyList())))
        .collect(Collectors.toList());

    return new PageResult<>(res.getTotal(), page, size, records);
  }

  @Override
  @Transactional
  public void updateOrderStatus(Long orderId, UpdateOrderStatusRequest req) {
    Order o = orderMapper.selectById(orderId);
    if (o == null) {
      throw ApiException.notFound("订单不存在: " + orderId);
    }

    OrderStatus current;
    OrderStatus target;
    try {
      current = OrderStatus.valueOf(o.getStatus());
      target = OrderStatus.valueOf(req.getStatus());
    } catch (IllegalArgumentException e) {
      throw ApiException.badRequest("非法订单状态: " + req.getStatus());
    }

    if (!current.canTransferTo(target)) {
      throw ApiException.conflict("状态不允许从 " + current + " -> " + target);
    }

    Order upd = new Order();
    upd.setId(orderId);
    upd.setStatus(target.name());

    int rows = orderMapper.updateById(upd);
    if (rows != 1) {
      throw ApiException.conflict("更新失败，请重试");
    }

    // 提交后 bump 版本（保证一致性）
    afterCommit(() -> orderDetailCacheService.bumpVersion(orderId));
  }

  private void afterCommit(Runnable r) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          r.run();
        }
      });
    } else {
      r.run();
    }
  }

  private OrderDetailVO toDetailVO(Order o, List<OrderItem> items) {
    OrderDetailVO vo = new OrderDetailVO();
    vo.setId(o.getId());
    vo.setUserId(o.getUserId());
    vo.setOrderNo(o.getOrderNo());
    vo.setTotalAmount(o.getTotalAmount());
    vo.setStatus(o.getStatus());
    vo.setRemark(o.getRemark());
    vo.setCreatedAt(o.getCreatedAt());

    List<OrderDetailVO.ItemVO> itemVos = items.stream().map(it -> {
      OrderDetailVO.ItemVO iv = new OrderDetailVO.ItemVO();
      iv.setId(it.getId());
      iv.setProductId(it.getProductId());
      iv.setProductName(it.getProductName());
      iv.setUnitPrice(it.getUnitPrice());
      iv.setQuantity(it.getQuantity());
      iv.setLineAmount(it.getLineAmount());
      return iv;
    }).collect(Collectors.toList());

    vo.setItems(itemVos);
    return vo;
  }
}

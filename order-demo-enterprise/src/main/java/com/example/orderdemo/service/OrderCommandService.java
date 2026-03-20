package com.example.orderdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.orderdemo.domain.dto.CreateOrderRequest;
import com.example.orderdemo.domain.entity.*;
import com.example.orderdemo.domain.enums.OrderEventType;
import com.example.orderdemo.domain.enums.OrderStatus;
import com.example.orderdemo.domain.enums.OutboxStatus;
import com.example.orderdemo.domain.event.OrderEventMessage;
import com.example.orderdemo.infrastructure.id.SnowflakeIdGenerator;
import com.example.orderdemo.infrastructure.json.Jsons;
import com.example.orderdemo.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderCommandService {

  private final UserMapper userMapper;
  private final OrderMapper orderMapper;
  private final OrderItemMapper orderItemMapper;
  private final OutboxEventMapper outboxEventMapper;

  // demo 固定 worker/datacenter，生产请配置化
  private final SnowflakeIdGenerator idGen = new SnowflakeIdGenerator(1, 1);

  public OrderCommandService(UserMapper userMapper,
                             OrderMapper orderMapper,
                             OrderItemMapper orderItemMapper,
                             OutboxEventMapper outboxEventMapper) {
    this.userMapper = userMapper;
    this.orderMapper = orderMapper;
    this.orderItemMapper = orderItemMapper;
    this.outboxEventMapper = outboxEventMapper;
  }

  @Transactional
  public long createOrder(CreateOrderRequest req) {
    UserEntity user = userMapper.selectById(req.getUserId());
    if (user == null) {
      throw new IllegalArgumentException("user not found: " + req.getUserId());
    }

    long orderId = idGen.nextId();
    LocalDateTime now = LocalDateTime.now();

    long total = req.getItems().stream()
        .mapToLong(i -> i.getPrice() * i.getQuantity())
        .sum();

    OrderEntity order = new OrderEntity();
    order.setId(orderId);
    order.setUserId(req.getUserId());
    order.setStatus(OrderStatus.CREATED.name());
    order.setTotalAmount(total);
    order.setCreatedAt(now);
    order.setUpdatedAt(now);
    orderMapper.insert(order);

    for (CreateOrderRequest.Item i : req.getItems()) {
      OrderItemEntity it = new OrderItemEntity();
      it.setId(idGen.nextId());
      it.setOrderId(orderId);
      it.setUserId(req.getUserId());
      it.setSkuId(i.getSkuId());
      it.setTitle(i.getTitle());
      it.setPrice(i.getPrice());
      it.setQuantity(i.getQuantity());
      it.setCreatedAt(now);
      orderItemMapper.insert(it);
    }

    // Outbox event：保证“写库 & 发送消息”最终一致（企业级标准做法）
    OrderEventMessage msg = new OrderEventMessage();
    msg.setEventType(OrderEventType.ORDER_UPSERT_V1.name());
    msg.setOrderId(orderId);
    msg.setUserId(req.getUserId());
    msg.setStatus(order.getStatus());
    msg.setTotalAmount(order.getTotalAmount());
    msg.setCreatedAt(order.getCreatedAt());
    msg.setUpdatedAt(order.getUpdatedAt());
    msg.setItems(req.getItems().stream().map(x -> {
      OrderEventMessage.Item ii = new OrderEventMessage.Item();
      ii.setSkuId(x.getSkuId());
      ii.setTitle(x.getTitle());
      ii.setPrice(x.getPrice());
      ii.setQuantity(x.getQuantity());
      return ii;
    }).collect(Collectors.toList()));

    OutboxEventEntity outbox = new OutboxEventEntity();
    outbox.setId(idGen.nextId());
    outbox.setAggregateId(orderId);
    outbox.setEventType(OrderEventType.ORDER_UPSERT_V1.name());
    outbox.setPayloadJson(Jsons.toJson(msg));
    outbox.setStatus(OutboxStatus.NEW.name());
    outbox.setRetryCount(0);
    outbox.setNextRetryAt(now);
    outbox.setCreatedAt(now);
    outbox.setUpdatedAt(now);
    outboxEventMapper.insert(outbox);

    log.info("order created orderId={}, userId={}, outboxId={}", orderId, req.getUserId(), outbox.getId());
    return orderId;
  }

  @Transactional
  public void updateStatus(long orderId, long userId, String newStatus) {
    OrderEntity order = orderMapper.selectById(orderId);
    if (order == null) throw new IllegalArgumentException("order not found: " + orderId);
    if (!order.getUserId().equals(userId)) throw new IllegalArgumentException("userId mismatch");

    order.setStatus(newStatus);
    order.setUpdatedAt(LocalDateTime.now());
    orderMapper.updateById(order);

    // 读取 items 组成 upsert 消息（确保 ES 是完整文档）
    var items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemEntity>()
        .eq(OrderItemEntity::getOrderId, orderId));

    OrderEventMessage msg = new OrderEventMessage();
    msg.setEventType(OrderEventType.ORDER_UPSERT_V1.name());
    msg.setOrderId(orderId);
    msg.setUserId(userId);
    msg.setStatus(newStatus);
    msg.setTotalAmount(order.getTotalAmount());
    msg.setCreatedAt(order.getCreatedAt());
    msg.setUpdatedAt(order.getUpdatedAt());
    msg.setItems(items.stream().map(x -> {
      OrderEventMessage.Item ii = new OrderEventMessage.Item();
      ii.setSkuId(x.getSkuId());
      ii.setTitle(x.getTitle());
      ii.setPrice(x.getPrice());
      ii.setQuantity(x.getQuantity());
      return ii;
    }).collect(Collectors.toList()));

    OutboxEventEntity outbox = new OutboxEventEntity();
    outbox.setId(idGen.nextId());
    outbox.setAggregateId(orderId);
    outbox.setEventType(OrderEventType.ORDER_UPSERT_V1.name());
    outbox.setPayloadJson(Jsons.toJson(msg));
    outbox.setStatus(OutboxStatus.NEW.name());
    outbox.setRetryCount(0);
    outbox.setNextRetryAt(LocalDateTime.now());
    outbox.setCreatedAt(LocalDateTime.now());
    outbox.setUpdatedAt(LocalDateTime.now());
    outboxEventMapper.insert(outbox);
  }
}

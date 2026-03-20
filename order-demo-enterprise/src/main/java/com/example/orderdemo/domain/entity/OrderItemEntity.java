package com.example.orderdemo.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_item")
public class OrderItemEntity {
  private Long id;
  private Long orderId;
  private Long userId;
  private Long skuId;
  private String title;
  private Long price;
  private Integer quantity;
  private LocalDateTime createdAt;
}

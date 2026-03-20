package com.example.orderdemo.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class OrderEntity {
  private Long id;
  private Long userId;
  private String status;
  private Long totalAmount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

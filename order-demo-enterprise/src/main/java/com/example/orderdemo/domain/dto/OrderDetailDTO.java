package com.example.orderdemo.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailDTO {
  private Long orderId;
  private Long userId;
  private String status;
  private Long totalAmount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<OrderItemDTO> items;
}

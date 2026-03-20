package com.example.orderdemo.domain.event;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderEventMessage {
  private String eventType;     // ORDER_UPSERT_V1
  private Long orderId;
  private Long userId;
  private String status;
  private Long totalAmount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<Item> items;

  @Data
  public static class Item {
    private Long skuId;
    private String title;
    private Long price;
    private Integer quantity;
  }
}

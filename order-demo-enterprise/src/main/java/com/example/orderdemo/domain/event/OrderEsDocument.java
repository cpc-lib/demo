package com.example.orderdemo.domain.event;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderEsDocument {
  private String orderId;
  private String userId;
  private String status;
  private Long totalAmount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<Item> items;

  @Data
  public static class Item {
    private String skuId;
    private String title;
    private Long price;
    private Integer quantity;
  }
}

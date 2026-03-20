package com.example.orderdemo.domain.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
  private Long skuId;
  private String title;
  private Long price;
  private Integer quantity;
}

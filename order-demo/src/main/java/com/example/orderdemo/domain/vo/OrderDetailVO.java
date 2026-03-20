package com.example.orderdemo.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {

  private Long id;
  private Long userId;
  private String orderNo;
  private BigDecimal totalAmount;
  private String status;
  private String remark;
  private LocalDateTime createdAt;

  private List<ItemVO> items;

  @Data
  public static class ItemVO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineAmount;
  }
}

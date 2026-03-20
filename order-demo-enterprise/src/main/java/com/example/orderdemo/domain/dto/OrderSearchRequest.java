package com.example.orderdemo.domain.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;

@Data
public class OrderSearchRequest {
  private Long userId;
  private String status;

  /** 金额范围（单位：分） */
  private Long minTotalAmount;
  private Long maxTotalAmount;

  /** 下单时间范围（createdAt） */
  private LocalDateTime createdAtFrom;
  private LocalDateTime createdAtTo;

  @Min(0)
  private Integer page = 0;

  @Min(1)
  private Integer size = 10;
}

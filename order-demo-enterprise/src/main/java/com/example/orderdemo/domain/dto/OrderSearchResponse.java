package com.example.orderdemo.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderSearchResponse {
  private long total;
  private List<OrderDetailDTO> orders;
}

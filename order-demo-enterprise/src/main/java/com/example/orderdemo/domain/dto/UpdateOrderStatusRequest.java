package com.example.orderdemo.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateOrderStatusRequest {
  @NotBlank
  private String status;
}

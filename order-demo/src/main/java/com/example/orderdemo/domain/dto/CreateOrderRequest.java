package com.example.orderdemo.domain.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {

  @NotNull
  private Long userId;

  @Size(max = 255)
  private String remark;

  @NotEmpty
  @Valid
  private List<Item> items;

  @Data
  public static class Item {

    @NotNull
    private Long productId;

    @NotBlank
    @Size(max = 128)
    private String productName;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 16, fraction = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Min(1)
    @Max(100000)
    private Integer quantity;
  }
}

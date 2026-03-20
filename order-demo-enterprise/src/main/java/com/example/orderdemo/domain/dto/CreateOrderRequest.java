package com.example.orderdemo.domain.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class CreateOrderRequest {

  @NotNull
  private Long userId;

  @NotEmpty
  @Valid
  private List<Item> items;

  @Data
  public static class Item {
    @NotNull
    private Long skuId;

    @NotBlank
    private String title;

    @NotNull @Min(1)
    private Long price;

    @NotNull @Min(1)
    private Integer quantity;
  }
}

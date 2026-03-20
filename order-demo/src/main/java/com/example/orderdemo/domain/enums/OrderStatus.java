package com.example.orderdemo.domain.enums;

public enum OrderStatus {
  CREATED,
  PAID,
  CANCELLED,
  SHIPPED,
  COMPLETED;

  public boolean canTransferTo(OrderStatus target) {
    if (this == target) return true;
    switch (this) {
      case CREATED:
        return target == PAID || target == CANCELLED;
      case PAID:
        return target == SHIPPED || target == CANCELLED;
      case SHIPPED:
        return target == COMPLETED;
      default:
        return false;
    }
  }
}

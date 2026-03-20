package com.example.orderdemo.infrastructure.cache;

public final class OrderCacheKeys {
  private OrderCacheKeys() {}

  public static String versionKey(long orderId) {
    return "order:ver:" + orderId;
  }

  public static String detailKey(long orderId, long ver) {
    return "order:detail:" + orderId + ":v" + ver;
  }
}

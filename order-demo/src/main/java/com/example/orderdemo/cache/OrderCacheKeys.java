package com.example.orderdemo.cache;

public class OrderCacheKeys {

  private OrderCacheKeys() {}

  public static String versionKey(Long orderId) {
    return "order:ver:" + orderId;
  }

  public static String detailKey(Long orderId, long ver) {
    return "order:detail:" + orderId + ":" + ver;
  }

  public static String lockKey(Long orderId) {
    return "lock:order:detail:" + orderId;
  }
}

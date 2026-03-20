package com.example.orderdemo.no;

public interface OrderNoGenerator {
  /** 返回“完整订单号”（含前缀），例如 OD1234567890... */
  String nextOrderNo();
}

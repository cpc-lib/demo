package com.example.fencing.model;

/**
 * 重置结果：恢复的商品数 + 清空的历史记录数。
 */
public record SeckillResetResponse(int products, int orders, int operations) { }

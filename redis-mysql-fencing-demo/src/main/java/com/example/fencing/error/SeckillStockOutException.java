package com.example.fencing.error;

public class SeckillStockOutException extends RuntimeException {
    public SeckillStockOutException(String productId) {
        super("Product sold out: " + productId);
    }
}

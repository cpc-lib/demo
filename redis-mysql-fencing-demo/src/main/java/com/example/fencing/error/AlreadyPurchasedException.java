package com.example.fencing.error;

public class AlreadyPurchasedException extends RuntimeException {
    public AlreadyPurchasedException(String productId, String userId) {
        super("User " + userId + " already purchased product " + productId);
    }
}

package com.example.fencing.model;

public record ProductSnapshot(
        String productId,
        String name,
        long stock,
        long sold,
        long fenceToken) { }

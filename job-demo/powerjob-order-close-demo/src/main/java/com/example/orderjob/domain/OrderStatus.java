package com.example.orderjob.domain;

import java.util.Arrays;

public enum OrderStatus {
    UNPAID(0),
    PAID(1),
    CLOSED(2);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static OrderStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown order status code: " + code));
    }
}

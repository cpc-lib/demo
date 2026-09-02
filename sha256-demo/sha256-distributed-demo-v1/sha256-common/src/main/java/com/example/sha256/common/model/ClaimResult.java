package com.example.sha256.common.model;

public enum ClaimResult {
    CLAIMED(1),
    LOCKED(0),
    NOT_FOUND(-2),
    TERMINAL(-3);

    private final long code;

    ClaimResult(long code) {
        this.code = code;
    }

    public static ClaimResult fromCode(Long code) {
        if (code == null) return LOCKED;
        for (ClaimResult value : values()) {
            if (value.code == code) return value;
        }
        return LOCKED;
    }
}

package com.example.orderjob.domain;

public record CloseResult(Long orderId, String orderNo, CloseOutcome outcome) {
}

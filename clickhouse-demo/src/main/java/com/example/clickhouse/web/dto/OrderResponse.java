package com.example.clickhouse.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderResponse {
    private long id;
    private long userId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;

    public OrderResponse() {}

    public OrderResponse(long id, long userId, BigDecimal amount, String status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

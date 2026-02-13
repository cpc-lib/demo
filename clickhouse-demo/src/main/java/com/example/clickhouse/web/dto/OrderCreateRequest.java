package com.example.clickhouse.web.dto;

import java.math.BigDecimal;

public class OrderCreateRequest {
    private long id;
    private long userId;
    private BigDecimal amount;
    private String status;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

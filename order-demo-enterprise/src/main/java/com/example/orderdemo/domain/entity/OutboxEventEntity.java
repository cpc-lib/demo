package com.example.orderdemo.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_outbox_event")
public class OutboxEventEntity {
  private Long id;
  private Long aggregateId;      // orderId
  private String eventType;      // ORDER_UPSERT_V1
  private String payloadJson;    // json string
  private String status;         // NEW/RETRY/SENT
  private LocalDateTime nextRetryAt;
  private Integer retryCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

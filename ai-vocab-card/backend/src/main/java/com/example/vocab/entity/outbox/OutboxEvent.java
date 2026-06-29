package com.example.vocab.entity.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("outbox_event")
public class OutboxEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private String status; // PENDING / SENT / FAILED
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime nextRetryTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

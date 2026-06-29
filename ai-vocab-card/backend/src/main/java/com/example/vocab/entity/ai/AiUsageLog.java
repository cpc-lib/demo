package com.example.vocab.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_usage_log")
public class AiUsageLog {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String model;
    private String requestType;
    private String inputText;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}

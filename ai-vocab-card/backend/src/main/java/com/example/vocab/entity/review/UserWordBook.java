package com.example.vocab.entity.review;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_word_book")
public class UserWordBook {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordCardId;
    private Integer masteryLevel;
    private Integer reviewCount;
    private Double easeFactor;
    private LocalDateTime lastReviewTime;
    private LocalDateTime nextReviewTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

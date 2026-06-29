package com.example.vocab.entity.review;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("word_review_log")
public class WordReviewLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long wordCardId;
    /** 0=forgot, 1=vague, 2=remembered */
    private Integer result;
    private Integer intervalDays;
    private Double easeFactor;
    private LocalDateTime reviewTime;
    private LocalDateTime nextReviewTime;
}

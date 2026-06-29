package com.example.vocab.entity.search;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("word_embedding")
public class WordEmbedding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long wordCardId;
    private String content;
    private String keywords;
    private String vectorProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.example.vocab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("word_slang")
public class WordSlang {
  @TableId(type = IdType.AUTO) private Long id;
  private Long wordCardId;
  private String phrase;
  private String meaning;
  private String example;
  private LocalDateTime createdAt;
}

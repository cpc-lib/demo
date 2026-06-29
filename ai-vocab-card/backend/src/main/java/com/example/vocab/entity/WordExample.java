package com.example.vocab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("word_example")
public class WordExample {
  @TableId(type = IdType.AUTO) private Long id;
  private Long wordCardId;
  private String sentence;
  private String translation;
  private String scene;
  private LocalDateTime createdAt;
}

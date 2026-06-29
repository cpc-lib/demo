package com.example.vocab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("word_card")
public class WordCard {
  @TableId(type = IdType.AUTO) private Long id;
  private String word;
  private String phonetic;
  private String partOfSpeech;
  private String englishDefinition;
  private String chineseMeaning;
  private String usageNote;
  private String tags;
  private Integer status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

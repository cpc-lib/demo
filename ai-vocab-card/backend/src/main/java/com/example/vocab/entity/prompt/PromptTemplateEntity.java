package com.example.vocab.entity.prompt;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("prompt_template")
public class PromptTemplateEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String version;
    private String title;
    private String content;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

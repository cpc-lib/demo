package com.example.vocab.entity.export;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("export_task")
public class ExportTask {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String exportType;
    private String status;
    private String fileName;
    private String fileUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.example.vocab.dto.export;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportTaskResponse {
    private Long taskId;
    private String exportType;
    private String status;
    private String fileName;
    private String fileUrl;
    private String errorMessage;
}

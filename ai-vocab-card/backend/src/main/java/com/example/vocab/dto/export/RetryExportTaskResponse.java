package com.example.vocab.dto.export;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RetryExportTaskResponse {
    private Long taskId;
    private String status;
}

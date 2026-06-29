package com.example.vocab.dto.export;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateExportTaskResponse {
    private Long taskId;
    private String status;
}

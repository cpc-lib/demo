package cc.ivera.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportTaskResponse {
    private String taskId;
    private String status;
    private String message;
    private String fileName;
    private String downloadUrl;
    private Long totalCount;
    private Long exportedCount;
}

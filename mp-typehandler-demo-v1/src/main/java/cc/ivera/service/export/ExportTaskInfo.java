package cc.ivera.service.export;

import cc.ivera.dto.UserProfileExportRequest;
import cc.ivera.enums.ExportTaskStatus;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class ExportTaskInfo {
    private String taskId;
    private ExportTaskStatus status;
    private String message;
    private String fileName;
    private Path filePath;
    private UserProfileExportRequest request;
    private long totalCount;
    private AtomicLong exportedCount = new AtomicLong(0);
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
}

package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("export_task")
public class ExportTaskRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String businessType;
    private String status;
    private String message;
    private String queryName;
    private Integer pageNo;
    private Integer pageSize;
    private String fieldsJson;
    private String fileName;
    private String filePath;
    private Long totalCount;
    private Long exportedCount;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime expireAt;
}

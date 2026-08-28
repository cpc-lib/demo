package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenant_data_deletion_stage")
public class TenantDataDeletionStage {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("task_id")
    private Long taskId;
    @TableField("stage_code")
    private String stageCode;
    @TableField("stage_status")
    private String stageStatus;
    @TableField("deleted_count")
    private Long deletedCount;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_message")
    private String errorMessage;
    @TableField("dry_run_result_json")
    private String dryRunResultJson;
    @TableField("verify_status")
    private String verifyStatus;
    @TableField("verify_result_json")
    private String verifyResultJson;
    @TableField("started_at")
    private LocalDateTime startedAt;
    @TableField("finished_at")
    private LocalDateTime finishedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_query_feedback")
public class RagQueryFeedback {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("query_log_id")
    private Long queryLogId;

    @TableField("rating")
    private String rating;

    @TableField("created_by")
    private String createdBy;

    @TableField("comment")
    private String comment;

    @TableField("corrected_answer")
    private String correctedAnswer;

    @TableField("feedback_status")
    private String feedbackStatus;

    @TableField("priority")
    private String priority;

    @TableField("assignee")
    private String assignee;

    @TableField("review_result")
    private String reviewResult;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("reopened_count")
    private Integer reopenedCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

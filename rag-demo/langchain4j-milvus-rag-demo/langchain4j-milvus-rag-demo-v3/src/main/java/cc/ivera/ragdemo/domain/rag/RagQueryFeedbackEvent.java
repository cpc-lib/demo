package cc.ivera.ragdemo.domain.rag;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_query_feedback_event")
public class RagQueryFeedbackEvent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField("feedback_id")
    private Long feedbackId;

    @TableField("event_type")
    private String eventType;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("operator")
    private String operator;

    @TableField("comment")
    private String comment;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

package cc.ivera.ragdemo.model.query;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RagFeedbackSummaryItem {

    private Long feedbackId;
    private Long queryLogId;
    private String rating;
    private String createdBy;
    private String comment;
    private String correctedAnswer;
    private String feedbackStatus;
    private String priority;
    private String assignee;
    private String reviewResult;
    private LocalDateTime feedbackCreatedAt;
    private Long tenantId;
    private String queryType;
    private String status;
    private String traceId;
    private String conversationId;
    private String queryText;
    private String retrievalMode;
    private Integer hitCount;
    private Integer totalTokens;
    private Long latencyMs;
    private LocalDateTime queryCreatedAt;
}

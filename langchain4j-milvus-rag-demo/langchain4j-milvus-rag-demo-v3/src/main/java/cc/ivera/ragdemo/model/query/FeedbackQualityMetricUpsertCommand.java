package cc.ivera.ragdemo.model.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FeedbackQualityMetricUpsertCommand {

    private final String table;
    private final Long tenantId;
    private final LocalDateTime bucketStart;
    private final String windowType;
    private final Long knowledgeBaseId;
    private final String retrievalMode;
    private final String queryType;
    private final String feedbackRating;
    private final String feedbackStatus;
    private final String assignee;
    private final int queryCount;
    private final long feedbackCount;
    private final long helpfulCount;
    private final long notHelpfulCount;
    private final long correctionCount;
}

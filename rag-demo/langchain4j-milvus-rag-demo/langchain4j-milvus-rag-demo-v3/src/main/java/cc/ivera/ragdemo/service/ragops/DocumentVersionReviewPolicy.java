package cc.ivera.ragdemo.service.ragops;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class DocumentVersionReviewPolicy {

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String PUBLISHED = "PUBLISHED";

    public String normalize(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : DRAFT;
    }

    public void assertCanSubmit(String status) {
        String normalized = normalize(status);
        if (!List.of(DRAFT, REJECTED).contains(normalized)) {
            throw new IllegalStateException("Only DRAFT or REJECTED document versions can be submitted for review");
        }
    }

    public void assertCanApproveOrReject(String status) {
        if (!PENDING_REVIEW.equals(normalize(status))) {
            throw new IllegalStateException("Only PENDING_REVIEW document versions can be approved or rejected");
        }
    }

    public void assertCanPublish(String status) {
        if (!APPROVED.equals(normalize(status))) {
            throw new IllegalStateException("Only APPROVED document versions can be published");
        }
    }
}

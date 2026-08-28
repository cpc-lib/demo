package cc.ivera.ragdemo.service.ragops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

@Component
public class ImageAssetReviewPolicy {

    @Autowired
    public ImageAssetReviewPolicy() {
    }

    public static final String EMPTY = "EMPTY";
    public static final String AUTO_APPROVED = "AUTO_APPROVED";
    public static final String REVIEW_PENDING = "REVIEW_PENDING";
    public static final String REVIEW_APPROVED = "REVIEW_APPROVED";
    public static final String REVIEW_REJECTED = "REVIEW_REJECTED";
    public static final String FAILED = "FAILED";

    private static final Set<String> STATUSES = Set.of(
            EMPTY, AUTO_APPROVED, REVIEW_PENDING, REVIEW_APPROVED, REVIEW_REJECTED, FAILED
    );

    public String initialStatus(boolean schemaValid,
                                Double visualConfidence,
                                Double ocrConfidence,
                                String embeddingStatus,
                                double lowConfidenceThreshold) {
        if ("FAILED".equals(normalizeNullable(embeddingStatus))) {
            return FAILED;
        }
        if (!schemaValid) {
            return REVIEW_PENDING;
        }
        if (belowThreshold(visualConfidence, lowConfidenceThreshold) || belowThreshold(ocrConfidence, lowConfidenceThreshold)) {
            return REVIEW_PENDING;
        }
        if (visualConfidence == null && ocrConfidence == null) {
            return EMPTY;
        }
        return AUTO_APPROVED;
    }

    public String approve(String currentStatus) {
        String current = normalize(currentStatus);
        if (FAILED.equals(current)) {
            throw new IllegalArgumentException("Failed image asset must be reprocessed before approval");
        }
        return REVIEW_APPROVED;
    }

    public String reject(String currentStatus) {
        String current = normalize(currentStatus);
        if (FAILED.equals(current)) {
            return FAILED;
        }
        return REVIEW_REJECTED;
    }

    public String update(String currentStatus) {
        String current = normalize(currentStatus);
        if (REVIEW_REJECTED.equals(current) || FAILED.equals(current)) {
            return REVIEW_PENDING;
        }
        return current;
    }

    public boolean participatesInDefaultRetrieval(String reviewStatus, boolean includeReviewPending) {
        String status = normalize(reviewStatus);
        if (AUTO_APPROVED.equals(status) || REVIEW_APPROVED.equals(status)) {
            return true;
        }
        return includeReviewPending && REVIEW_PENDING.equals(status);
    }

    public String normalize(String status) {
        if (!StringUtils.hasText(status)) {
            return EMPTY;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported image asset review status: " + status);
        }
        return normalized;
    }

    private String normalizeNullable(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : null;
    }

    private boolean belowThreshold(Double value, double threshold) {
        return value != null && value < Math.max(0.0D, Math.min(1.0D, threshold));
    }
}

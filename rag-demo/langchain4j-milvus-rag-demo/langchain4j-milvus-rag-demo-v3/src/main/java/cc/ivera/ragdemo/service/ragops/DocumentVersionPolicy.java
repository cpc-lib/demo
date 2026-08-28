package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.domain.rag.DocumentVersionStatus;

public final class DocumentVersionPolicy {

    private DocumentVersionPolicy() {
    }

    public static int nextVersionNo(Integer currentVersionNo) {
        return currentVersionNo == null || currentVersionNo < 1 ? 1 : currentVersionNo + 1;
    }

    public static boolean canBecomeCurrent(DocumentVersionStatus status) {
        return status == DocumentVersionStatus.PROCESSING || status == DocumentVersionStatus.AVAILABLE;
    }

    public static void assertCanBecomeCurrent(DocumentVersionStatus status) {
        if (!canBecomeCurrent(status)) {
            throw new IllegalStateException("Document version status cannot become current: " + status);
        }
    }
}

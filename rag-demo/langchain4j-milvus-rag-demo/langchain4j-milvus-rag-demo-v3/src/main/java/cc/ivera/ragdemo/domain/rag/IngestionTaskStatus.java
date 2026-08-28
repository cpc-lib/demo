package cc.ivera.ragdemo.domain.rag;

public enum IngestionTaskStatus {
    PENDING(0),
    RUNNING(1),
    SUCCESS(2),
    FAILED(3),
    RETRY_WAIT(4),
    CANCELLED(5),
    PARTIAL_SUCCESS(6);

    private final int code;

    IngestionTaskStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static IngestionTaskStatus fromCode(Integer code) {
        if (code == null) {
            return PENDING;
        }
        for (IngestionTaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ingestion task status code: " + code);
    }
}

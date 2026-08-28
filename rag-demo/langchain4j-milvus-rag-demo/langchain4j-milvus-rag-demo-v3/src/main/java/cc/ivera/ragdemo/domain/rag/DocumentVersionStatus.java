package cc.ivera.ragdemo.domain.rag;

public enum DocumentVersionStatus {
    PROCESSING(0),
    AVAILABLE(1),
    FAILED(2),
    DISABLED(3),
    DELETED(4);

    private final int code;

    DocumentVersionStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static DocumentVersionStatus fromCode(Integer code) {
        if (code == null) {
            return PROCESSING;
        }
        for (DocumentVersionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown document version status code: " + code);
    }
}

package cc.ivera.gray.common;

public final class GrayEnums {
    private GrayEnums() {
    }

    public enum RuleType {
        USER,
        TENANT,
        HEADER,
        COOKIE,
        IP,
        APP_VERSION,
        REGION,
        PERCENT
    }

    public enum ReleaseStrategy {
        CANARY,
        BLUE_GREEN,
        AB_TEST
    }

    public enum ReleaseStatus {
        WAITING_APPROVAL,
        DRAFT,
        RUNNING,
        PAUSED,
        COMPLETED,
        ROLLED_BACK,
        REJECTED
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum AlertLevel {
        INFO,
        WARN,
        CRITICAL
    }
}

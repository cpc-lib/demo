package cc.ivera.ragdemo.admin;

public enum TenantDeletionExecutionMode {
    DRY_RUN,
    EXECUTE;

    public static TenantDeletionExecutionMode normalize(String value) {
        if (value == null || value.isBlank()) {
            return DRY_RUN;
        }
        for (TenantDeletionExecutionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported tenant deletion execution mode: " + value);
    }
}

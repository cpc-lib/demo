package cc.ivera.ragdemo.admin;

public record TenantDeletionStageResult(
        String stageCode,
        String status,
        long affectedCount,
        String errorCode,
        String message,
        String detailJson
) {

    public static TenantDeletionStageResult success(String stageCode, long affectedCount, String detailJson) {
        return new TenantDeletionStageResult(stageCode, "SUCCESS", Math.max(0, affectedCount), null, null,
                detailJson == null ? "{}" : detailJson);
    }

    public static TenantDeletionStageResult skipped(String stageCode, String reason) {
        return new TenantDeletionStageResult(stageCode, "SKIPPED", 0, null, reason,
                "{\"reason\":\"" + escape(reason) + "\"}");
    }

    public static TenantDeletionStageResult failed(String stageCode, String errorCode, String message) {
        return new TenantDeletionStageResult(stageCode, "FAILED", 0, errorCode, message,
                "{\"error\":\"" + escape(message) + "\"}");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

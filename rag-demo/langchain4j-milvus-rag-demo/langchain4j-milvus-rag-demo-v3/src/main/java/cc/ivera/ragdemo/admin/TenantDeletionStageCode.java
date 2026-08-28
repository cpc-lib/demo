package cc.ivera.ragdemo.admin;

import java.util.List;

public enum TenantDeletionStageCode {
    MYSQL,
    OBJECT_STORAGE,
    MILVUS,
    ELASTICSEARCH,
    REDIS,
    SESSION,
    AUDIT_RETENTION;

    public static List<String> defaultStageCodes() {
        return List.of(MYSQL.name(), OBJECT_STORAGE.name(), MILVUS.name(), ELASTICSEARCH.name(), REDIS.name(), SESSION.name(), AUDIT_RETENTION.name());
    }
}

package cc.ivera.ragdemo.tenant;

import org.springframework.stereotype.Component;

@Component
public class TenantScopedRedisKeyFactory {

    public String chunkRecordKey(Long tenantId, String chunkId, int version) {
        return chunkRecordKey(String.valueOf(safeTenant(tenantId)), chunkId, version);
    }

    public String chunkRecordKey(String tenantId, String chunkId, int version) {
        return "rag:%s:knowledge:chunk:%s:v:%s".formatted(safeTenant(tenantId), chunkId, version);
    }

    public String chunkVersionsKey(Long tenantId, String chunkId) {
        return chunkVersionsKey(String.valueOf(safeTenant(tenantId)), chunkId);
    }

    public String chunkVersionsKey(String tenantId, String chunkId) {
        return "rag:%s:knowledge:chunk:versions:%s".formatted(safeTenant(tenantId), chunkId);
    }

    public String chunkActiveVersionKey(Long tenantId, String chunkId) {
        return chunkActiveVersionKey(String.valueOf(safeTenant(tenantId)), chunkId);
    }

    public String chunkActiveVersionKey(String tenantId, String chunkId) {
        return "rag:%s:knowledge:chunk:active:%s".formatted(safeTenant(tenantId), chunkId);
    }

    public String documentChunksKey(Long tenantId, String documentId) {
        return documentChunksKey(String.valueOf(safeTenant(tenantId)), documentId);
    }

    public String documentChunksKey(String tenantId, String documentId) {
        return "rag:%s:knowledge:document:%s:chunks".formatted(safeTenant(tenantId), documentId);
    }

    public String storeChunksKey(Long tenantId, String alias, String collection) {
        return storeChunksKey(String.valueOf(safeTenant(tenantId)), alias, collection);
    }

    public String storeChunksKey(String tenantId, String alias, String collection) {
        return "rag:%s:knowledge:store:%s:%s:chunks".formatted(safeTenant(tenantId), alias, collection);
    }

    public String queryKey(Long tenantId, String userId, String queryHash) {
        return "query:%s:%s:%s".formatted(safeTenant(tenantId), userId, queryHash);
    }

    private long safeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String safeTenant(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "0" : tenantId.trim();
    }
}

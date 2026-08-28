package cc.ivera.ragdemo.tenant;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantScopedObjectKeyFactory {

    public String originalObjectKey(String environment, Long tenantId, Long knowledgeBaseId, String documentRef, String fileHash, String fileName) {
        return scopedKey(environment, tenantId, knowledgeBaseId, documentRef, "original", fileHash, fileName);
    }

    public String versionObjectKey(String environment, Long tenantId, Long knowledgeBaseId, String documentRef, Integer versionNo, String fileHash, String fileName) {
        return scopedKey(environment, tenantId, knowledgeBaseId, documentRef, "versions/v" + (versionNo == null ? 0 : versionNo), fileHash, fileName);
    }

    public String assetObjectKey(String environment, Long tenantId, Long knowledgeBaseId, String documentRef, String imageId, String extension) {
        return scopedKey(environment, tenantId, knowledgeBaseId, documentRef, "assets", null,
                sanitize(StringUtils.hasText(imageId) ? imageId : "image") + "." + sanitize(StringUtils.hasText(extension) ? extension : "png"));
    }

    public String scopedKey(String environment,
                            Long tenantId,
                            Long knowledgeBaseId,
                            String documentRef,
                            String category,
                            String fileHash,
                            String fileName) {
        return "%s/%s/%s/%s/%s/%s%s".formatted(
                sanitize(StringUtils.hasText(environment) ? environment : "default"),
                tenantId == null ? 0 : tenantId,
                knowledgeBaseId == null ? 0 : knowledgeBaseId,
                sanitize(StringUtils.hasText(documentRef) ? documentRef : "unknown-document"),
                sanitize(StringUtils.hasText(category) ? category : "objects"),
                StringUtils.hasText(fileHash) ? sanitize(fileHash) + "/" : "",
                sanitize(fileName)
        );
    }

    public boolean belongsToTenant(String objectKey, Long tenantId) {
        if (!StringUtils.hasText(objectKey) || tenantId == null) {
            return false;
        }
        String normalized = objectKey.replace("\\", "/");
        String[] parts = normalized.split("/");
        return parts.length > 1 && String.valueOf(tenantId).equals(parts[1]);
    }

    private String sanitize(String value) {
        String raw = StringUtils.hasText(value) ? value.trim() : "unknown";
        return raw.replaceAll("[\\\\:*?\"<>|\\r\\n]", "_").replace("/", "_");
    }
}

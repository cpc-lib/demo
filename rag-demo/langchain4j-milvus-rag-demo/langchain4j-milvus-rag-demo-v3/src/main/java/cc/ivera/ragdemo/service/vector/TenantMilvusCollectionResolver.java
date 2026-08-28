package cc.ivera.ragdemo.service.vector;

import cc.ivera.ragdemo.tenant.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class TenantMilvusCollectionResolver {

    private static final int MAX_COLLECTION_NAME_LENGTH = 255;
    private static final String DEFAULT_COLLECTION = "rag_collection";
    private static final Pattern INVALID_COLLECTION_CHARS = Pattern.compile("[^A-Za-z0-9_]");

    public String currentTenantCollection(String baseCollection) {
        return collectionForTenant(baseCollection, TenantContextHolder.currentTenantId().orElse(null));
    }

    public String collectionForTenant(String baseCollection, Long tenantId) {
        String sanitizedBase = sanitizeBaseCollection(baseCollection);
        if (tenantId == null || tenantId <= 0) {
            return sanitizedBase;
        }
        String suffix = "_tenant_" + tenantId;
        int maxBaseLength = Math.max(1, MAX_COLLECTION_NAME_LENGTH - suffix.length());
        String trimmedBase = sanitizedBase.length() > maxBaseLength
                ? sanitizedBase.substring(0, maxBaseLength)
                : sanitizedBase;
        return trimmedBase + suffix;
    }

    public boolean belongsToTenant(String collectionName, Long tenantId) {
        if (!StringUtils.hasText(collectionName) || tenantId == null || tenantId <= 0) {
            return false;
        }
        return collectionName.trim().toLowerCase(Locale.ROOT).endsWith(("_tenant_" + tenantId).toLowerCase(Locale.ROOT));
    }

    private String sanitizeBaseCollection(String baseCollection) {
        String value = StringUtils.hasText(baseCollection) ? baseCollection.trim() : DEFAULT_COLLECTION;
        value = INVALID_COLLECTION_CHARS.matcher(value).replaceAll("_");
        if (value.isBlank()) {
            value = DEFAULT_COLLECTION;
        }
        char first = value.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            value = "c_" + value;
        }
        if (value.length() > MAX_COLLECTION_NAME_LENGTH) {
            value = value.substring(0, MAX_COLLECTION_NAME_LENGTH);
        }
        return value;
    }
}

package cc.ivera.ragdemo.permission;

import java.util.Locale;

public enum KnowledgeBaseRole {
    READER(10),
    EDITOR(20),
    ADMIN(30),
    OWNER(40);

    private final int level;

    KnowledgeBaseRole(int level) {
        this.level = level;
    }

    public boolean atLeast(KnowledgeBaseRole required) {
        return level >= required.level;
    }

    public static KnowledgeBaseRole from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return KnowledgeBaseRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

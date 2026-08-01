package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.config.RagProperties;

public final class ChunkRegistryMaintenancePolicy {

    private ChunkRegistryMaintenancePolicy() {
    }

    public static boolean shouldRebuildOnStartup(RagProperties.ChunkRegistry properties) {
        return properties != null
                && properties.isEnabled()
                && properties.isRebuildOnStartup();
    }

    public static boolean shouldRebuildOnSchedule(RagProperties.ChunkRegistry properties) {
        return properties != null
                && properties.isEnabled()
                && properties.isScheduledRebuildEnabled();
    }
}

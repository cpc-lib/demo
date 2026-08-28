package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.ChunkRedisRebuildResponse;
import cc.ivera.ragdemo.service.ragops.ChunkRegistryMaintenancePolicy;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class ChunkRedisRegistryMaintenanceService implements ApplicationRunner {

    private final RagProperties properties;
    private final KnowledgeChunkManagementService chunkManagementService;

    @Override
    public void run(ApplicationArguments args) {
        RagProperties.ChunkRegistry registryProperties = properties.getChunkRegistry();
        if (ChunkRegistryMaintenancePolicy.shouldRebuildOnStartup(registryProperties)) {
            rebuild("startup", registryProperties);
        }
    }

    @Scheduled(
            initialDelayString = "${rag.chunk-registry.rebuild-initial-delay-millis:3600000}",
            fixedDelayString = "${rag.chunk-registry.rebuild-fixed-delay-millis:3600000}"
    )
    public void scheduledRebuild() {
        RagProperties.ChunkRegistry registryProperties = properties.getChunkRegistry();
        if (ChunkRegistryMaintenancePolicy.shouldRebuildOnSchedule(registryProperties)) {
            rebuild("scheduled", registryProperties);
        }
    }

    private void rebuild(String reason, RagProperties.ChunkRegistry registryProperties) {
        TenantContextHolder.runWith(
                TenantContextHolder.systemContext(properties.getIngestion().getDefaultTenantId(), "chunk-registry-rebuild:" + reason),
                () -> {
                    try {
                        ChunkRedisRebuildResponse response = chunkManagementService.rebuildRedisRegistryFromMysql(
                                registryProperties.isClearExistingOnRebuild());
                        log.info("Chunk Redis registry rebuild completed. reason={}, mysqlRows={}, chunkCount={}, activeCount={}, deletedRedisKeys={}",
                                reason,
                                response.mysqlRows(),
                                response.chunkCount(),
                                response.activeCount(),
                                response.deletedRedisKeys());
                    } catch (Exception e) {
                        log.warn("Chunk Redis registry rebuild failed. reason={}", reason, e);
                    }
                });
    }
}

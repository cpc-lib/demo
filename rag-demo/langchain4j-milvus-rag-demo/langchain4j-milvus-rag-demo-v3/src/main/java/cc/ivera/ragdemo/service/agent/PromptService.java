package cc.ivera.ragdemo.service.agent;

import cc.ivera.ragdemo.domain.rag.RagAgentPrompt;
import cc.ivera.ragdemo.mapper.RagAgentPromptMapper;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Provides agent system prompts with MySQL source + Redis cache-aside.
 * <p>
 * Read path: Redis cache -> MySQL active prompt -> global default (tenant_id=0).
 * Write path: MySQL insert/update + Redis cache eviction.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class PromptService {

    private static final String CACHE_KEY_PREFIX = "rag:%d:agent:system-prompt";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final String DEFAULT_PROMPT_NAME = "default";

    private final RagAgentPromptMapper promptMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /**
     * Get the active system prompt for the current tenant.
     * Falls back to global (tenant_id=0) if no tenant-specific prompt exists.
     *
     * @return the system prompt text
     */
    public String getActiveSystemPrompt() {
        Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
        return getActiveSystemPrompt(tenantId);
    }

    /**
     * Get the active system prompt for a specific tenant.
     * Cache-aside: try Redis first, fall back to MySQL, then global default.
     *
     * @param tenantId the tenant ID
     * @return the system prompt text
     */
    public String getActiveSystemPrompt(Long tenantId) {
        String cacheKey = CACHE_KEY_PREFIX.formatted(tenantId);
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();

        // Try Redis cache
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed for prompt key={}, falling back to MySQL", cacheKey, e);
            }
        }

        // Cache miss - load from MySQL
        String prompt = loadFromMysql(tenantId);

        // Write to cache
        if (redisTemplate != null && prompt != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, prompt, CACHE_TTL);
            } catch (Exception e) {
                log.warn("Redis cache write failed for prompt key={}", cacheKey, e);
            }
        }

        return prompt;
    }

    /**
     * Create a new prompt version for a tenant.
     * Deactivates previous active version, inserts new active version, evicts cache.
     *
     * @param tenantId     the tenant ID (0 for global)
     * @param promptName   the prompt name
     * @param content      the prompt content
     * @param updatedBy    the operator user ID
     * @return the created prompt entity
     */
    public RagAgentPrompt updatePrompt(Long tenantId, String promptName, String content, String updatedBy) {
        return createPrompt(tenantId, promptName, content, true, updatedBy);
    }

    /**
     * Create a prompt version. When enabled, it becomes the only active prompt
     * for the tenant and prompt name. When disabled, the current active prompt
     * is left untouched.
     */
    public RagAgentPrompt createPrompt(Long tenantId, String promptName, String content, Boolean enabled, String updatedBy) {
        return TenantContextHolder.callWithBypass(() ->
                createPromptInternal(tenantId, normalizePromptName(promptName), requireContent(content), Boolean.TRUE.equals(enabled), updatedBy));
    }

    /**
     * Edit a tenant-owned prompt. Active prompt edits are immutable: they create
     * a new active version and deactivate the old one. Disabled prompt edits
     * update the row in place unless the caller asks to enable it.
     */
    public RagAgentPrompt updatePromptById(
            Long tenantId,
            Long id,
            String promptName,
            String content,
            Boolean enabled,
            String updatedBy
    ) {
        return TenantContextHolder.callWithBypass(() -> {
            RagAgentPrompt existing = requireTenantOwnedPrompt(tenantId, id);
            String normalizedName = normalizePromptName(promptName != null ? promptName : existing.getPromptName());
            String normalizedContent = requireContent(content);

            if (Integer.valueOf(1).equals(existing.getStatus())) {
                return createPromptInternal(tenantId, normalizedName, normalizedContent, true, updatedBy);
            }

            boolean enableAfterEdit = Boolean.TRUE.equals(enabled);
            if (enableAfterEdit) {
                deactivateActivePrompts(tenantId, normalizedName, updatedBy, existing.getId());
            }
            existing.setPromptName(normalizedName);
            existing.setPromptContent(normalizedContent);
            existing.setStatus(enableAfterEdit ? 1 : 0);
            existing.setUpdatedBy(updatedBy);
            existing.setUpdatedAt(LocalDateTime.now());
            promptMapper.updateById(existing);
            if (enableAfterEdit) {
                evictCache(tenantId);
            }
            log.info("Agent prompt edited: id={}, tenant={}, name={}, enabled={}",
                    id, tenantId, normalizedName, enableAfterEdit);
            return existing;
        });
    }

    /**
     * Enable one prompt and disable the other active prompt for this tenant.
     */
    public RagAgentPrompt enablePrompt(Long tenantId, Long id, String updatedBy) {
        return TenantContextHolder.callWithBypass(() -> {
            RagAgentPrompt target = requireTenantOwnedPrompt(tenantId, id);
            deactivateActivePrompts(tenantId, target.getPromptName(), updatedBy, target.getId());
            target.setStatus(1);
            target.setUpdatedBy(updatedBy);
            target.setUpdatedAt(LocalDateTime.now());
            promptMapper.updateById(target);
            evictCache(tenantId);
            log.info("Agent prompt enabled: id={}, tenant={}, name={}, version={}",
                    id, tenantId, target.getPromptName(), target.getVersion());
            return target;
        });
    }

    /**
     * Disable a tenant-owned prompt. Runtime falls back to global default when
     * no tenant prompt remains active.
     */
    public RagAgentPrompt disablePrompt(Long tenantId, Long id, String updatedBy) {
        return TenantContextHolder.callWithBypass(() -> {
            RagAgentPrompt target = requireTenantOwnedPrompt(tenantId, id);
            if (!Integer.valueOf(0).equals(target.getStatus())) {
                target.setStatus(0);
                target.setUpdatedBy(updatedBy);
                target.setUpdatedAt(LocalDateTime.now());
                promptMapper.updateById(target);
                evictCache(tenantId);
            }
            log.info("Agent prompt disabled: id={}, tenant={}, name={}, version={}",
                    id, tenantId, target.getPromptName(), target.getVersion());
            return target;
        });
    }

    /**
     * Get the active prompt for a tenant (for admin display).
     */
    public RagAgentPrompt getActivePrompt(Long tenantId) {
        return TenantContextHolder.callWithBypass(() -> {
            RagAgentPrompt prompt = findActive(tenantId, DEFAULT_PROMPT_NAME);
            if (prompt != null || tenantId == 0) {
                return prompt;
            }
            return findActive(0L, DEFAULT_PROMPT_NAME);
        });
    }

    /**
     * List all prompt versions for a tenant.
     */
    public java.util.List<RagAgentPrompt> listVersions(Long tenantId) {
        return TenantContextHolder.callWithBypass(() -> promptMapper.selectList(new LambdaQueryWrapper<RagAgentPrompt>()
                .eq(RagAgentPrompt::getTenantId, tenantId)
                .eq(RagAgentPrompt::getPromptName, DEFAULT_PROMPT_NAME)
                .orderByDesc(RagAgentPrompt::getVersion)));
    }

    /**
     * List all tenant-owned system prompt records for management.
     */
    public List<RagAgentPrompt> listPrompts(Long tenantId) {
        return listVersions(tenantId);
    }

    /**
     * Roll back to a specific version by activating it and deactivating the current one.
     */
    public RagAgentPrompt rollbackToVersion(Long tenantId, Integer version, String updatedBy) {
        return TenantContextHolder.callWithBypass(() -> {
            RagAgentPrompt target = promptMapper.selectOne(new LambdaQueryWrapper<RagAgentPrompt>()
                    .eq(RagAgentPrompt::getTenantId, tenantId)
                    .eq(RagAgentPrompt::getPromptName, DEFAULT_PROMPT_NAME)
                    .eq(RagAgentPrompt::getVersion, version));
            if (target == null) {
                throw new IllegalArgumentException("Prompt version not found: tenant=" + tenantId + ", version=" + version);
            }

            // Deactivate current active
            RagAgentPrompt current = findActive(tenantId, DEFAULT_PROMPT_NAME);
            if (current != null && !current.getId().equals(target.getId())) {
                current.setStatus(0);
                current.setUpdatedBy(updatedBy);
                current.setUpdatedAt(LocalDateTime.now());
                promptMapper.updateById(current);
            }

            // Activate target
            target.setStatus(1);
            target.setUpdatedBy(updatedBy);
            target.setUpdatedAt(LocalDateTime.now());
            promptMapper.updateById(target);

            evictCache(tenantId);
            log.info("Agent prompt rollback: tenant={}, version={}", tenantId, version);
            return target;
        });
    }

    /**
     * Evict the cached prompt for a tenant.
     */
    public void evictCache(Long tenantId) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(CACHE_KEY_PREFIX.formatted(tenantId));
            } catch (Exception e) {
                log.warn("Redis cache eviction failed for tenant={}", tenantId, e);
            }
        }
    }

    private String loadFromMysql(Long tenantId) {
        // Use bypass: the service itself handles tenant resolution (tenant-specific -> global fallback).
        // Without bypass, the interceptor would block the global (tenant_id=0) fallback query.
        return TenantContextHolder.callWithBypass(() -> {
            // Try tenant-specific active prompt
            RagAgentPrompt prompt = findActive(tenantId, DEFAULT_PROMPT_NAME);
            if (prompt != null) {
                return prompt.getPromptContent();
            }
            // Fall back to global default (tenant_id=0)
            if (tenantId != 0) {
                prompt = findActive(0L, DEFAULT_PROMPT_NAME);
                if (prompt != null) {
                    return prompt.getPromptContent();
                }
            }
            // Ultimate fallback - should not happen if seed data exists
            log.warn("No active system prompt found for tenant={}, using empty default", tenantId);
            return "";
        });
    }

    private RagAgentPrompt findActive(Long tenantId, String promptName) {
        return promptMapper.selectOne(new LambdaQueryWrapper<RagAgentPrompt>()
                .eq(RagAgentPrompt::getTenantId, tenantId)
                .eq(RagAgentPrompt::getPromptName, promptName)
                .eq(RagAgentPrompt::getStatus, 1)
                .last("LIMIT 1"));
    }

    private RagAgentPrompt createPromptInternal(
            Long tenantId,
            String promptName,
            String content,
            boolean enabled,
            String updatedBy
    ) {
        if (enabled) {
            deactivateActivePrompts(tenantId, promptName, updatedBy, null);
        }

        int newVersion = nextVersion(tenantId, promptName);
        RagAgentPrompt prompt = new RagAgentPrompt();
        prompt.setTenantId(tenantId);
        prompt.setPromptName(promptName);
        prompt.setPromptContent(content);
        prompt.setVersion(newVersion);
        prompt.setStatus(enabled ? 1 : 0);
        prompt.setCreatedBy(updatedBy);
        prompt.setUpdatedBy(updatedBy);
        promptMapper.insert(prompt);

        if (enabled) {
            evictCache(tenantId);
        }
        log.info("Agent prompt created: tenant={}, name={}, version={}, enabled={}",
                tenantId, promptName, newVersion, enabled);
        return prompt;
    }

    private int nextVersion(Long tenantId, String promptName) {
        return promptMapper.selectList(new LambdaQueryWrapper<RagAgentPrompt>()
                        .eq(RagAgentPrompt::getTenantId, tenantId)
                        .eq(RagAgentPrompt::getPromptName, promptName))
                .stream()
                .mapToInt(RagAgentPrompt::getVersion)
                .max()
                .orElse(0) + 1;
    }

    private void deactivateActivePrompts(Long tenantId, String promptName, String updatedBy, Long excludedId) {
        List<RagAgentPrompt> activePrompts = promptMapper.selectList(new LambdaQueryWrapper<RagAgentPrompt>()
                .eq(RagAgentPrompt::getTenantId, tenantId)
                .eq(RagAgentPrompt::getPromptName, promptName)
                .eq(RagAgentPrompt::getStatus, 1));
        for (RagAgentPrompt active : activePrompts) {
            if (excludedId != null && excludedId.equals(active.getId())) {
                continue;
            }
            active.setStatus(0);
            active.setUpdatedBy(updatedBy);
            active.setUpdatedAt(LocalDateTime.now());
            promptMapper.updateById(active);
        }
    }

    private RagAgentPrompt requireTenantOwnedPrompt(Long tenantId, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Prompt id is required");
        }
        RagAgentPrompt prompt = promptMapper.selectById(id);
        if (prompt == null || !tenantId.equals(prompt.getTenantId())) {
            throw new IllegalArgumentException("Prompt not found: id=" + id);
        }
        return prompt;
    }

    private String normalizePromptName(String promptName) {
        if (promptName == null || promptName.isBlank()) {
            return DEFAULT_PROMPT_NAME;
        }
        return promptName.trim();
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("promptContent is required");
        }
        return content;
    }
}

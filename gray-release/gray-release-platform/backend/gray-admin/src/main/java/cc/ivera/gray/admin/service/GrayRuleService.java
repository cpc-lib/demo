package cc.ivera.gray.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.gray.admin.entity.GrayRule;
import cc.ivera.gray.admin.mapper.GrayRuleMapper;
import cc.ivera.gray.common.GrayEnums.RuleType;
import cc.ivera.gray.common.GrayMatchRequest;
import cc.ivera.gray.common.GrayMatchResult;
import cc.ivera.gray.common.VersionCompare;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrayRuleService {
    private static final Duration RULE_CACHE_TTL = Duration.ofMinutes(5);

    private final GrayRuleMapper grayRuleMapper;
    private final AuditService auditService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ServicePolicyService servicePolicyService;
    private final NacosRulePublisher nacosRulePublisher;

    public GrayRuleService(GrayRuleMapper grayRuleMapper,
                           AuditService auditService,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           ServicePolicyService servicePolicyService,
                           NacosRulePublisher nacosRulePublisher) {
        this.grayRuleMapper = grayRuleMapper;
        this.auditService = auditService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.servicePolicyService = servicePolicyService;
        this.nacosRulePublisher = nacosRulePublisher;
    }

    public List<GrayRule> list(String serviceId) {
        LambdaQueryWrapper<GrayRule> wrapper = new LambdaQueryWrapper<GrayRule>()
                .orderByAsc(GrayRule::getPriority)
                .orderByDesc(GrayRule::getUpdateTime);
        if (serviceId != null && !serviceId.isBlank()) {
            wrapper.eq(GrayRule::getServiceId, serviceId);
        }
        return grayRuleMapper.selectList(wrapper);
    }

    @Transactional
    public GrayRule create(GrayRule rule, String operator) {
        normalize(rule);
        List<GrayRule> conflicts = findConflicts(rule, null);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("存在冲突规则：" + conflicts.get(0).getRuleName());
        }
        grayRuleMapper.insert(rule);
        evictRuleCache(rule.getServiceId());
        publishRules(rule.getServiceId());
        auditService.record(operator, "CREATE", "GRAY_RULE", String.valueOf(rule.getId()), null, rule.getRuleName());
        return rule;
    }

    @Transactional
    public GrayRule update(Long id, GrayRule rule, String operator) {
        GrayRule before = grayRuleMapper.selectById(id);
        if (before == null) {
            throw new IllegalArgumentException("灰度规则不存在");
        }
        rule.setId(id);
        normalize(rule);
        List<GrayRule> conflicts = findConflicts(rule, id);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("存在冲突规则：" + conflicts.get(0).getRuleName());
        }
        grayRuleMapper.updateById(rule);
        evictRuleCache(rule.getServiceId());
        publishRules(rule.getServiceId());
        auditService.record(operator, "UPDATE", "GRAY_RULE", String.valueOf(id), before.getRuleName(), rule.getRuleName());
        return grayRuleMapper.selectById(id);
    }

    @Transactional
    public void delete(Long id, String operator) {
        GrayRule before = grayRuleMapper.selectById(id);
        if (before == null) {
            return;
        }
        grayRuleMapper.deleteById(id);
        evictRuleCache(before.getServiceId());
        publishRules(before.getServiceId());
        auditService.record(operator, "DELETE", "GRAY_RULE", String.valueOf(id), before.getRuleName(), null);
    }

    public GrayMatchResult match(GrayMatchRequest request) {
        List<GrayRule> rules = activeRules(request.getServiceId());

        for (GrayRule rule : rules) {
            if (matches(rule, request)) {
                GrayMatchResult result = new GrayMatchResult();
                result.setServiceId(request.getServiceId());
                result.setTargetVersion(rule.getTargetVersion());
                result.setMatched(true);
                result.setRuleId(rule.getId());
                result.setRuleName(rule.getRuleName());
                result.setReason("命中规则：" + rule.getRuleName());
                return result;
            }
        }
        String defaultVersion = defaultVersion(request);
        return GrayMatchResult.defaultVersion(request.getServiceId(), defaultVersion);
    }

    public List<GrayRule> findConflicts(GrayRule candidate, Long excludeId) {
        String ruleType = candidate.getRuleType() == null ? null : candidate.getRuleType().toUpperCase(Locale.ROOT);
        LambdaQueryWrapper<GrayRule> wrapper = new LambdaQueryWrapper<GrayRule>()
                .eq(GrayRule::getEnabled, true)
                .eq(GrayRule::getServiceId, candidate.getServiceId())
                .eq(GrayRule::getRuleType, ruleType)
                .eq(GrayRule::getConditionKey, candidate.getConditionKey())
                .eq(GrayRule::getConditionValue, candidate.getConditionValue())
                .orderByAsc(GrayRule::getPriority);
        if (excludeId != null) {
            wrapper.ne(GrayRule::getId, excludeId);
        }
        return grayRuleMapper.selectList(wrapper);
    }

    private List<GrayRule> activeRules(String serviceId) {
        String key = cacheKey(serviceId);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, new TypeReference<>() {
                });
            }
        } catch (Exception ignored) {
            // Redis 故障时降级读库，不能影响网关放行。
        }

        List<GrayRule> rules = grayRuleMapper.selectList(new LambdaQueryWrapper<GrayRule>()
                .eq(GrayRule::getServiceId, serviceId)
                .eq(GrayRule::getEnabled, true)
                .orderByAsc(GrayRule::getPriority)
                .orderByAsc(GrayRule::getId));
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(rules), RULE_CACHE_TTL);
        } catch (Exception ignored) {
            // 缓存写入失败不影响主流程。
        }
        return rules;
    }

    public boolean publishRules(String serviceId) {
        return nacosRulePublisher.publish(serviceId, activeRules(serviceId));
    }

    private String defaultVersion(GrayMatchRequest request) {
        var policy = servicePolicyService.getOrCreate(request.getServiceId());
        if (Boolean.TRUE.equals(policy.getAbEnabled())) {
            int bucket = bucket(request.getUserId(), request.getTenantId(), request.getIp());
            if (bucket < safePercent(policy.getAbPercentB())) {
                return policy.getAbVersionB();
            }
            return policy.getAbVersionA();
        }
        return policy.getDefaultVersion() == null ? "v1" : policy.getDefaultVersion();
    }

    private void evictRuleCache(String serviceId) {
        try {
            redisTemplate.delete(cacheKey(serviceId));
        } catch (Exception ignored) {
            // 缓存清理失败时依赖 TTL 兜底。
        }
    }

    private String cacheKey(String serviceId) {
        return "gray:rules:" + serviceId;
    }

    private void normalize(GrayRule rule) {
        if (rule.getPriority() == null) {
            rule.setPriority(100);
        }
        if (rule.getTrafficPercent() == null) {
            rule.setTrafficPercent(0);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }
        rule.setRuleType(rule.getRuleType().toUpperCase(Locale.ROOT));
    }

    private boolean matches(GrayRule rule, GrayMatchRequest request) {
        RuleType type = RuleType.valueOf(rule.getRuleType());
        return switch (type) {
            case USER -> equalsAny(request.getUserId(), rule.getConditionValue());
            case TENANT -> equalsAny(request.getTenantId(), rule.getConditionValue());
            case IP -> equalsAny(request.getIp(), rule.getConditionValue());
            case REGION -> equalsAny(request.getRegion(), rule.getConditionValue());
            case HEADER -> equalsAny(getIgnoreCase(request.getHeaders(), rule.getConditionKey()), rule.getConditionValue());
            case COOKIE -> equalsAny(getIgnoreCase(request.getCookies(), rule.getConditionKey()), rule.getConditionValue());
            case APP_VERSION -> VersionCompare.compare(request.getAppVersion(), rule.getConditionValue()) >= 0;
            case PERCENT -> bucket(request.getUserId(), request.getTenantId(), request.getIp()) < safePercent(rule.getTrafficPercent());
        };
    }

    private int safePercent(Integer percent) {
        if (percent == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, percent));
    }

    private String getIgnoreCase(Map<String, String> values, String key) {
        if (values == null || key == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean equalsAny(String actual, String expectedCsv) {
        if (actual == null || expectedCsv == null) {
            return false;
        }
        String trimmedActual = actual.trim();
        for (String expected : expectedCsv.split(",")) {
            if (trimmedActual.equalsIgnoreCase(expected.trim())) {
                return true;
            }
        }
        return false;
    }

    private int bucket(String userId, String tenantId, String ip) {
        String seed = firstNotBlank(userId, tenantId, ip, "anonymous");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hex = HexFormat.of().formatHex(digest.digest(seed.getBytes(StandardCharsets.UTF_8)));
            long value = Long.parseUnsignedLong(hex.substring(0, 8), 16);
            return (int) (value % 100);
        } catch (NoSuchAlgorithmException ex) {
            return Math.abs(seed.hashCode()) % 100;
        }
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "anonymous";
    }
}

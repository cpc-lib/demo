package cc.ivera.gray.common;

public class GrayMatchResult {
    private String serviceId;
    private String targetVersion;
    private boolean matched;
    private Long ruleId;
    private String ruleName;
    private String reason;

    public static GrayMatchResult defaultVersion(String serviceId, String version) {
        GrayMatchResult result = new GrayMatchResult();
        result.setServiceId(serviceId);
        result.setTargetVersion(version);
        result.setMatched(false);
        result.setReason("未命中灰度规则，使用默认版本");
        return result;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}


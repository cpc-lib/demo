package cc.ivera.ragdemo.service.ragops;

public class TenantQuotaPolicy {

    public void assertWithinLimit(String metric, long requested, long current, long limit) {
        if (limit <= 0) {
            return;
        }
        if (current + requested > limit) {
            throw new IllegalStateException("Tenant quota exceeded for " + metric + ": " + (current + requested) + " > " + limit);
        }
    }

    public long safeLimit(Long configured, long defaultValue) {
        return configured == null || configured <= 0 ? defaultValue : configured;
    }
}

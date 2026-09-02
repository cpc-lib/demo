package com.example.sha256.api.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sha256.upload")
public class MultipartUploadProperties {
    private int partSizeMb = 16;
    private int recommendedConcurrency = 4;
    private int presignExpireMinutes = 30;
    private int sessionTtlHours = 24;
    private int completedResumeMinutes = 10;
    private int maxPresignBatch = 100;

    public int getPartSizeMb() { return partSizeMb; }
    public void setPartSizeMb(int partSizeMb) { this.partSizeMb = partSizeMb; }
    public int getRecommendedConcurrency() { return recommendedConcurrency; }
    public void setRecommendedConcurrency(int recommendedConcurrency) { this.recommendedConcurrency = recommendedConcurrency; }
    public int getPresignExpireMinutes() { return presignExpireMinutes; }
    public void setPresignExpireMinutes(int presignExpireMinutes) { this.presignExpireMinutes = presignExpireMinutes; }
    public int getSessionTtlHours() { return sessionTtlHours; }
    public void setSessionTtlHours(int sessionTtlHours) { this.sessionTtlHours = sessionTtlHours; }
    public int getCompletedResumeMinutes() { return completedResumeMinutes; }
    public void setCompletedResumeMinutes(int completedResumeMinutes) { this.completedResumeMinutes = completedResumeMinutes; }
    public int getMaxPresignBatch() { return maxPresignBatch; }
    public void setMaxPresignBatch(int maxPresignBatch) { this.maxPresignBatch = maxPresignBatch; }
}

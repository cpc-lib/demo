package cc.ivera.gray.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("release_task")
public class ReleaseTask {
    @TableId
    private Long id;
    private String taskName;
    private String serviceId;
    private String fromVersion;
    private String targetVersion;
    private String strategy;
    private Integer currentPercent;
    private String status;
    private String owner;
    private String stagesJson;
    private Boolean autoRollbackEnabled;
    private Double errorRateThreshold;
    private Integer p99LatencyThresholdMs;
    private Double latestErrorRate;
    private Integer latestP99LatencyMs;
    private String healthStatus;
    private String rollbackReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(String fromVersion) {
        this.fromVersion = fromVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Integer getCurrentPercent() {
        return currentPercent;
    }

    public void setCurrentPercent(Integer currentPercent) {
        this.currentPercent = currentPercent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getStagesJson() {
        return stagesJson;
    }

    public void setStagesJson(String stagesJson) {
        this.stagesJson = stagesJson;
    }

    public Boolean getAutoRollbackEnabled() {
        return autoRollbackEnabled;
    }

    public void setAutoRollbackEnabled(Boolean autoRollbackEnabled) {
        this.autoRollbackEnabled = autoRollbackEnabled;
    }

    public Double getErrorRateThreshold() {
        return errorRateThreshold;
    }

    public void setErrorRateThreshold(Double errorRateThreshold) {
        this.errorRateThreshold = errorRateThreshold;
    }

    public Integer getP99LatencyThresholdMs() {
        return p99LatencyThresholdMs;
    }

    public void setP99LatencyThresholdMs(Integer p99LatencyThresholdMs) {
        this.p99LatencyThresholdMs = p99LatencyThresholdMs;
    }

    public Double getLatestErrorRate() {
        return latestErrorRate;
    }

    public void setLatestErrorRate(Double latestErrorRate) {
        this.latestErrorRate = latestErrorRate;
    }

    public Integer getLatestP99LatencyMs() {
        return latestP99LatencyMs;
    }

    public void setLatestP99LatencyMs(Integer latestP99LatencyMs) {
        this.latestP99LatencyMs = latestP99LatencyMs;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getRollbackReason() {
        return rollbackReason;
    }

    public void setRollbackReason(String rollbackReason) {
        this.rollbackReason = rollbackReason;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

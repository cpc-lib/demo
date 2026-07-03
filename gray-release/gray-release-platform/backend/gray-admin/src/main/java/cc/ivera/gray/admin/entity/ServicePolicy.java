package cc.ivera.gray.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("service_policy")
public class ServicePolicy {
    @TableId
    private Long id;
    private String serviceId;
    private String defaultVersion;
    private String blueVersion;
    private String greenVersion;
    private String activeColor;
    private Boolean abEnabled;
    private String abVersionA;
    private String abVersionB;
    private Integer abPercentB;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getBlueVersion() {
        return blueVersion;
    }

    public void setBlueVersion(String blueVersion) {
        this.blueVersion = blueVersion;
    }

    public String getGreenVersion() {
        return greenVersion;
    }

    public void setGreenVersion(String greenVersion) {
        this.greenVersion = greenVersion;
    }

    public String getActiveColor() {
        return activeColor;
    }

    public void setActiveColor(String activeColor) {
        this.activeColor = activeColor;
    }

    public Boolean getAbEnabled() {
        return abEnabled;
    }

    public void setAbEnabled(Boolean abEnabled) {
        this.abEnabled = abEnabled;
    }

    public String getAbVersionA() {
        return abVersionA;
    }

    public void setAbVersionA(String abVersionA) {
        this.abVersionA = abVersionA;
    }

    public String getAbVersionB() {
        return abVersionB;
    }

    public void setAbVersionB(String abVersionB) {
        this.abVersionB = abVersionB;
    }

    public Integer getAbPercentB() {
        return abPercentB;
    }

    public void setAbPercentB(Integer abPercentB) {
        this.abPercentB = abPercentB;
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


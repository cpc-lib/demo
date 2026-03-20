package cc.ivera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(
        prefix = "cpc.mybatis"
)
public class MybatisProperties {

    private String locationPattern;

    private String typeEnumsPackage;

    private String typeAliasesPackage;

    public String getLocationPattern() {
        return locationPattern;
    }

    public void setLocationPattern(String locationPattern) {
        this.locationPattern = locationPattern;
    }

    public String getTypeEnumsPackage() {
        return typeEnumsPackage;
    }

    public void setTypeEnumsPackage(String typeEnumsPackage) {
        this.typeEnumsPackage = typeEnumsPackage;
    }

    public String getTypeAliasesPackage() {
        return typeAliasesPackage;
    }

    public void setTypeAliasesPackage(String typeAliasesPackage) {
        this.typeAliasesPackage = typeAliasesPackage;
    }
}

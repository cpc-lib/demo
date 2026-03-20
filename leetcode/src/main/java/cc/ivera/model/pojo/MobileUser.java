package cc.ivera.model.pojo;

import lombok.Data;
import cc.ivera.annotation.Desensitization;
import cc.ivera.annotation.SensitiveData;
import cc.ivera.enums.DesensitizationType;

@Data
public class MobileUser {

    //@Desensitization(type = DesensitizationType.ID_CARD, prefixLen = 4, suffixLen = 14)
    @SensitiveData(prefixLen = 4, suffixLen = 14)
    private String cardId;

    @Desensitization(type = DesensitizationType.CHINESE_NAME)
    private String name;

    @Desensitization(type = DesensitizationType.MOBILE_PHONE)
    private String phone;

    @Desensitization(type = DesensitizationType.CUSTOMIZE_RULE, prefixLen = 3, suffixLen = 6)
    private String info;
}

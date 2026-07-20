package cc.ivera.enums.reconciliation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DiscrepancyStatus {

    OPEN("待处理"),

    RESOLVED("已处理"),

    AUTO_RESOLVED("自动处理");

    private final String type;
}

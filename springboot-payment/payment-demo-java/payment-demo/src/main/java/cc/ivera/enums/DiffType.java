package cc.ivera.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DiffType {

    MATCH("完全匹配"),

    MISSING_LOCAL("漏单（渠道有，本地无）"),

    MISSING_CHANNEL("多单（本地有，渠道无）"),

    AMOUNT_MISMATCH("金额不符"),

    STATUS_MISMATCH("状态不符");

    private final String description;
}

package cc.ivera.enums.reconciliation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MatchStatus {

    MATCHED("匹配"),

    MISMATCH("不匹配"),

    CHANNEL_ONLY("仅渠道有"),

    LOCAL_ONLY("仅本地有");

    private final String type;
}

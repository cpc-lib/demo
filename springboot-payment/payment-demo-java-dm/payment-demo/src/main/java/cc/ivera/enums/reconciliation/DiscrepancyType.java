package cc.ivera.enums.reconciliation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DiscrepancyType {

    OVERPAYMENT("多付款"),

    UNDERPAYMENT("少付款"),

    AMOUNT_MISMATCH("金额不一致"),

    STATUS_MISMATCH("状态不一致");

    private final String type;
}

package cc.ivera.ordermachine.domain.enums;

public enum OrderEvent {
    PAY,
    SHIP,
    CONFIRM_RECEIPT,
    CANCEL,
    TIMEOUT_CLOSE,
    APPLY_REFUND,
    REFUND_SUCCESS,
    REFUND_REJECT
}

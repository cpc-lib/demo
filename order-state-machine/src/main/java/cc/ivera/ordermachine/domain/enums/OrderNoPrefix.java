package cc.ivera.ordermachine.domain.enums;

/**
 * 订单号业务类型前缀
 */
public enum OrderNoPrefix {

    ORDER("ORD"),
    PAY("PAY"),
    REFUND("REF"),
    AFTER_SALE("AFT");

    private final String code;

    OrderNoPrefix(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
package cc.ivera.support;

public final class PaymentLockKeys {

    private PaymentLockKeys() {
    }

    public static String wxPayOrder(String orderNo) {
        return "payment:lock:wxpay:order:" + orderNo;
    }

    /**
     * 主动退款同步和退款回调必须按同一退款单号串行处理。
     */
    public static String refund(String refundNo) {
        return "payment:lock:refund:" + refundNo;
    }
}

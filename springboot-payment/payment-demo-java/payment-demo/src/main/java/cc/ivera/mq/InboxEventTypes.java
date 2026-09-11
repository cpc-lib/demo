package cc.ivera.mq;

public final class InboxEventTypes {

    public static final String PAYMENT_NOTIFY_WX_V2 = "PAYMENT_NOTIFY_WX_V2";

    public static final String PAYMENT_NOTIFY_WX_V3 = "PAYMENT_NOTIFY_WX_V3";

    public static final String PAYMENT_NOTIFY_ALIPAY = "PAYMENT_NOTIFY_ALIPAY";

    public static final String REFUND_NOTIFY_WX_V3 = "REFUND_NOTIFY_WX_V3";

    public static final String PAYMENT_QUERY_WX = "PAYMENT_QUERY_WX";

    public static final String PAYMENT_QUERY_ALIPAY = "PAYMENT_QUERY_ALIPAY";

    public static final String REFUND_QUERY_WX = "REFUND_QUERY_WX";

    public static final String REFUND_QUERY_ALIPAY = "REFUND_QUERY_ALIPAY";

    private InboxEventTypes() {
    }
}

package cc.ivera.mq;

public final class OutboxEventTypes {

    public static final String ORDER_CLOSE_SCHEDULED = "ORDER_CLOSE_SCHEDULED";

    public static final String REFUND_SUBMIT_REQUESTED = "REFUND_SUBMIT_REQUESTED";

    public static final String REFUND_STATUS_SYNC_REQUESTED = "REFUND_STATUS_SYNC_REQUESTED";

    private OutboxEventTypes() {
    }
}

package cc.ivera.service;

public interface RefundStatusSyncMessageService {

    void sendRefundStatusSyncMessage(String refundNo);

    default void sendRefundStatusSyncMessage(String refundNo, int attempt) {
        if (attempt != 0) {
            throw new IllegalArgumentException("legacy refund status sender only supports attempt 0");
        }
        sendRefundStatusSyncMessage(refundNo);
    }
}


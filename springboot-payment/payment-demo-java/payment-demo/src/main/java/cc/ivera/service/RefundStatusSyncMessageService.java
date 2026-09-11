package cc.ivera.service;

public interface RefundStatusSyncMessageService {

    default void sendRefundStatusSyncMessage(String refundNo) {
        sendRefundStatusSyncMessage(refundNo, 0);
    }

    void sendRefundStatusSyncMessage(String refundNo, int attempt);
}


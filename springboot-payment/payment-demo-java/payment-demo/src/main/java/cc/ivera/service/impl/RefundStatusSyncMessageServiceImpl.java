package cc.ivera.service.impl;

import cc.ivera.mq.OutboxEventTypes;
import cc.ivera.service.MessageOutboxService;
import cc.ivera.service.RefundStatusSyncMessageService;
import cc.ivera.util.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RefundStatusSyncMessageServiceImpl implements RefundStatusSyncMessageService {

    private final MessageOutboxService messageOutboxService;

    public RefundStatusSyncMessageServiceImpl(MessageOutboxService messageOutboxService) {
        this.messageOutboxService = messageOutboxService;
    }

    @Override
    public void sendRefundStatusSyncMessage(String refundNo, int attempt) {
        if (!StringUtils.hasText(refundNo)) {
            throw new IllegalArgumentException("refundNo must not be blank");
        }
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must not be negative");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("refundNo", refundNo);
        payload.put("attempt", attempt);
        messageOutboxService.insertOnce(
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED + ":" + refundNo + ":" + attempt,
                "REFUND",
                refundNo,
                OutboxEventTypes.REFUND_STATUS_SYNC_REQUESTED,
                JsonUtils.toJson(payload)
        );
    }
}


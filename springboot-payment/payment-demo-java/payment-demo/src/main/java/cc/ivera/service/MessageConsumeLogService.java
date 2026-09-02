package cc.ivera.service;

import java.util.Date;

public interface MessageConsumeLogService {

    MessageConsumeClaim tryStart(String eventId, String consumerName, String eventType, String businessKey);

    void complete(String eventId, String consumerName, String leaseToken);

    void fail(String eventId, String consumerName, String leaseToken, String error, Date retryAfter);
}

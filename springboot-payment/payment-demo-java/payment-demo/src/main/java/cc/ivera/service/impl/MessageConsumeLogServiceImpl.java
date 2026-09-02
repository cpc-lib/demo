package cc.ivera.service.impl;

import cc.ivera.entity.MessageConsumeLog;
import cc.ivera.exception.ConflictException;
import cc.ivera.mapper.MessageConsumeLogMapper;
import cc.ivera.service.MessageConsumeClaim;
import cc.ivera.service.MessageConsumeLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class MessageConsumeLogServiceImpl implements MessageConsumeLogService {

    private final MessageConsumeLogMapper mapper;

    private final String tokenPrefix;

    private final long leaseMillis;

    public MessageConsumeLogServiceImpl(
            MessageConsumeLogMapper mapper,
            @Value("${payment.inbox.worker-id:}") String configuredWorkerId,
            @Value("${payment.inbox.lease-seconds:30}") long leaseSeconds
    ) {
        this.mapper = mapper;
        this.tokenPrefix = StringUtils.hasText(configuredWorkerId)
                ? configuredWorkerId
                : UUID.randomUUID().toString();
        this.leaseMillis = Math.max(1L, leaseSeconds) * 1000L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageConsumeClaim tryStart(String eventId,
                                        String consumerName,
                                        String eventType,
                                        String businessKey) {
        requireText(eventId, "消费事件ID不能为空");
        requireText(consumerName, "消费者名称不能为空");
        requireText(eventType, "消费事件类型不能为空");
        requireText(businessKey, "消费业务键不能为空");
        requireMaxLength(eventId, 36, "消费事件ID长度不能超过36");
        requireMaxLength(consumerName, 64, "消费者名称长度不能超过64");
        requireMaxLength(eventType, 64, "消费事件类型长度不能超过64");
        requireMaxLength(businessKey, 128, "消费业务键长度不能超过128");

        String leaseToken = leaseToken();
        MessageConsumeLog log = new MessageConsumeLog();
        log.setEventId(eventId);
        log.setConsumerName(consumerName);
        log.setEventType(eventType);
        log.setBusinessKey(businessKey);
        log.setStatus("PROCESSING");
        log.setLockedBy(leaseToken);
        try {
            if (mapper.insert(log, leaseSeconds()) == 1) {
                return MessageConsumeClaim.claimed(leaseToken);
            }
            throw new ConflictException("消费事件写入失败");
        } catch (DuplicateKeyException duplicate) {
            // 唯一键竞争由下方当前读和租约条件更新裁决。
        }

        MessageConsumeLog existing = mapper.selectByEventAndConsumer(eventId, consumerName);
        if (existing == null) {
            throw new ConflictException("消费事件并发状态不明确，请重试");
        }
        validateSameEvent(existing, eventType, businessKey);
        if ("CONSUMED".equals(existing.getStatus())) {
            return MessageConsumeClaim.consumed();
        }
        boolean reclaimed = mapper.reclaimExpiredOrFailed(
                eventId,
                consumerName,
                leaseToken,
                leaseSeconds()
        ) == 1;
        return reclaimed ? MessageConsumeClaim.claimed(leaseToken) : MessageConsumeClaim.busy();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(String eventId, String consumerName, String leaseToken) {
        requireText(eventId, "消费事件ID不能为空");
        requireText(consumerName, "消费者名称不能为空");
        requireText(leaseToken, "消费租约不能为空");
        requireMaxLength(eventId, 36, "消费事件ID长度不能超过36");
        requireMaxLength(consumerName, 64, "消费者名称长度不能超过64");
        if (mapper.markConsumed(eventId, consumerName, leaseToken, new Date()) != 1) {
            throw new ConflictException("消费租约已失效，不能提交完成状态");
        }
    }

    @Override
    public void fail(String eventId,
                     String consumerName,
                     String leaseToken,
                     String error,
                     Date retryAfter) {
        requireText(eventId, "消费事件ID不能为空");
        requireText(consumerName, "消费者名称不能为空");
        requireText(leaseToken, "消费租约不能为空");
        requireMaxLength(eventId, 36, "消费事件ID长度不能超过36");
        requireMaxLength(consumerName, 64, "消费者名称长度不能超过64");
        String safeError = StringUtils.hasText(error) ? error : "消息消费失败";
        if (safeError.length() > 1000) {
            safeError = safeError.substring(0, 1000);
        }
        Date safeRetryAfter = retryAfter == null
                ? new Date(System.currentTimeMillis() + 1000L)
                : retryAfter;
        mapper.markFailed(eventId, consumerName, leaseToken, safeError, safeRetryAfter);
    }

    private void validateSameEvent(MessageConsumeLog existing,
                                   String eventType,
                                   String businessKey) {
        if (!Objects.equals(existing.getEventType(), eventType)
                || !Objects.equals(existing.getBusinessKey(), businessKey)) {
            throw new ConflictException("消费事件参数冲突");
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireMaxLength(String value, int maxLength, String message) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    private String leaseToken() {
        String prefix = tokenPrefix.length() > 20 ? tokenPrefix.substring(0, 20) : tokenPrefix;
        return prefix + ":" + UUID.randomUUID();
    }

    private long leaseSeconds() {
        return Math.max(1L, leaseMillis / 1000L);
    }
}

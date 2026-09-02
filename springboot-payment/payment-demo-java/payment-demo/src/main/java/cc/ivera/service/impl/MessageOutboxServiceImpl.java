package cc.ivera.service.impl;

import cc.ivera.entity.MessageOutbox;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.NotFoundException;
import cc.ivera.mapper.MessageOutboxMapper;
import cc.ivera.service.MessageOutboxService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;

@Service
public class MessageOutboxServiceImpl implements MessageOutboxService {

    private final MessageOutboxMapper mapper;

    public MessageOutboxServiceImpl(MessageOutboxMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageOutbox insertOnce(String eventKey,
                                    String aggregateType,
                                    String aggregateId,
                                    String eventType,
                                    String payload) {
        requireText(eventKey, "消息业务键不能为空");
        requireText(aggregateType, "消息聚合类型不能为空");
        requireText(aggregateId, "消息聚合标识不能为空");
        requireText(eventType, "消息事件类型不能为空");
        requireText(payload, "消息内容不能为空");
        requireMaxLength(eventKey, 128, "消息业务键长度不能超过128");
        requireMaxLength(aggregateType, 32, "消息聚合类型长度不能超过32");
        requireMaxLength(aggregateId, 64, "消息聚合标识长度不能超过64");
        requireMaxLength(eventType, 64, "消息事件类型长度不能超过64");

        MessageOutbox event = new MessageOutbox();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventKey(eventKey);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus("NEW");
        event.setRetryCount(0);
        try {
            if (mapper.insert(event) != 1) {
                throw new ConflictException("消息事件写入失败");
            }
            return event;
        } catch (DuplicateKeyException duplicate) {
            MessageOutbox existing = mapper.selectByEventKey(eventKey);
            if (existing == null) {
                throw duplicate;
            }
            validateSameEvent(existing, event);
            return existing;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryFailed(String eventId) {
        requireText(eventId, "消息事件ID不能为空");
        requireMaxLength(eventId, 36, "消息事件ID长度不能超过36");
        MessageOutbox event = mapper.selectByEventId(eventId);
        if (event == null) {
            throw new NotFoundException("消息事件不存在");
        }
        if (!"FAILED".equals(event.getStatus())) {
            throw new ConflictException("只有失败消息可以重投");
        }
        if (mapper.resetFailed(eventId) != 1) {
            throw new ConflictException("消息状态已变化，请刷新后重试");
        }
    }

    private void validateSameEvent(MessageOutbox existing, MessageOutbox requested) {
        if (!Objects.equals(existing.getEventKey(), requested.getEventKey())
                || !Objects.equals(existing.getAggregateType(), requested.getAggregateType())
                || !Objects.equals(existing.getAggregateId(), requested.getAggregateId())
                || !Objects.equals(existing.getEventType(), requested.getEventType())
                || !Objects.equals(existing.getPayload(), requested.getPayload())) {
            throw new ConflictException("消息业务键参数冲突");
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
}

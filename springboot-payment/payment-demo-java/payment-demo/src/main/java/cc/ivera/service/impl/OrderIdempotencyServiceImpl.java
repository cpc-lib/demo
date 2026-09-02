package cc.ivera.service.impl;

import cc.ivera.dto.OrderIdempotencyKeyView;
import cc.ivera.entity.OrderIdempotency;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.mapper.OrderIdempotencyMapper;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderIdempotencyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderIdempotencyServiceImpl implements OrderIdempotencyService {

    private static final String ISSUED = "ISSUED";
    private static final String COMPLETED = "COMPLETED";
    private static final String EXPIRED = "EXPIRED";
    private static final Duration UNUSED_KEY_RETENTION = Duration.ofDays(7);

    private final OrderIdempotencyMapper mapper;
    private final long ttlSeconds;

    public OrderIdempotencyServiceImpl(
            OrderIdempotencyMapper mapper,
            @Value("${payment.order.idempotency-key-ttl-seconds:120}") long ttlSeconds
    ) {
        this.mapper = mapper;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderIdempotencyKeyView issue(AuthUser user) {
        if (user == null || user.getRole() != UserRole.USER) {
            throw new ForbiddenException("管理员账号不参与购物");
        }
        Date expiresAt = new Date(System.currentTimeMillis() + ttlSeconds * 1_000L);
        String key = UUID.randomUUID().toString();
        OrderIdempotency record = new OrderIdempotency();
        record.setUserId(user.getUserId());
        record.setIdempotencyKey(key);
        record.setStatus(ISSUED);
        record.setExpiresAt(expiresAt);
        if (mapper.insert(record) != 1) {
            throw new BizException("订单幂等键签发失败");
        }
        return new OrderIdempotencyKeyView(key, expiresAt);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void expireIssuedKeyIfNeeded(Long userId, String idempotencyKey) {
        mapper.expireIssuedKeyIfNeeded(userId, idempotencyKey, new Date());
    }

    @Override
    public OrderIdempotency requireForUpdate(
            Long userId,
            String idempotencyKey,
            String requestFingerprint
    ) {
        OrderIdempotency record = mapper.selectByKeyForUpdate(idempotencyKey);
        if (record == null || !Objects.equals(userId, record.getUserId())) {
            throw new ConflictException("订单幂等键无效");
        }
        if (COMPLETED.equals(record.getStatus())) {
            if (!Objects.equals(requestFingerprint, record.getRequestFingerprint())) {
                throw new ConflictException("订单幂等键请求参数不一致");
            }
            return record;
        }
        if (EXPIRED.equals(record.getStatus())
                || record.getExpiresAt() == null
                || !record.getExpiresAt().after(new Date())) {
            throw new ConflictException("订单幂等键已过期");
        }
        if (!ISSUED.equals(record.getStatus())) {
            throw new ConflictException("订单幂等键状态无效");
        }
        return record;
    }

    @Override
    public void complete(Long id, String requestFingerprint, Long orderId) {
        if (mapper.completeIssued(id, requestFingerprint, orderId, new Date()) != 1) {
            throw new ConflictException("订单幂等键已被使用");
        }
    }

    @Override
    @Scheduled(cron = "${payment.order.idempotency-cleanup-cron:0 0 3 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredUnusedKeys() {
        Date cutoff = new Date(System.currentTimeMillis() - UNUSED_KEY_RETENTION.toMillis());
        mapper.deleteUnusedExpiredBefore(cutoff);
    }
}

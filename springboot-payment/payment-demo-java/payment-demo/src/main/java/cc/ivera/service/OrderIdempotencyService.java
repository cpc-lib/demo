package cc.ivera.service;

import cc.ivera.dto.OrderIdempotencyKeyView;
import cc.ivera.entity.OrderIdempotency;
import cc.ivera.security.AuthUser;

public interface OrderIdempotencyService {

    OrderIdempotencyKeyView issue(AuthUser user);

    void expireIssuedKeyIfNeeded(Long userId, String idempotencyKey);

    OrderIdempotency requireForUpdate(Long userId, String idempotencyKey, String requestFingerprint);

    void complete(Long id, String requestFingerprint, Long orderId);

    void cleanupExpiredUnusedKeys();
}

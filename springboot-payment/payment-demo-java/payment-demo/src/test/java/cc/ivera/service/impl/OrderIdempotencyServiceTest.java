package cc.ivera.service.impl;

import cc.ivera.dto.OrderIdempotencyKeyView;
import cc.ivera.entity.OrderIdempotency;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.mapper.OrderIdempotencyMapper;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderIdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderIdempotencyServiceTest {

    private OrderIdempotencyMapper mapper;
    private OrderIdempotencyService service;

    @BeforeEach
    void setUp() {
        mapper = mock(OrderIdempotencyMapper.class);
        service = new OrderIdempotencyServiceImpl(mapper, 120L);
    }

    @Test
    void userReceivesServerGeneratedUuidBoundForOneHundredTwentySeconds() {
        when(mapper.insert(any(OrderIdempotency.class))).thenReturn(1);
        long before = System.currentTimeMillis();

        OrderIdempotencyKeyView view = service.issue(
                new AuthUser(7L, "alice", UserRole.USER)
        );

        long after = System.currentTimeMillis();
        UUID.fromString(view.getIdempotencyKey());
        assertTrue(view.getExpiresAt().getTime() >= before + 120_000L);
        assertTrue(view.getExpiresAt().getTime() <= after + 120_000L);
        ArgumentCaptor<OrderIdempotency> captor = ArgumentCaptor.forClass(OrderIdempotency.class);
        verify(mapper).insert(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals("ISSUED", captor.getValue().getStatus());
        assertEquals(view.getIdempotencyKey(), captor.getValue().getIdempotencyKey());
        assertEquals(view.getExpiresAt(), captor.getValue().getExpiresAt());
    }

    @Test
    void adminCannotReceiveAnOrderKey() {
        assertThrows(ForbiddenException.class,
                () -> service.issue(new AuthUser(1L, "admin", UserRole.ADMIN)));

        verify(mapper, never()).insert(any(OrderIdempotency.class));
    }

    @Test
    void missingOrCrossUserKeyIsRejectedAsConflict() {
        when(mapper.selectByKeyForUpdate("missing")).thenReturn(null);
        OrderIdempotency otherUser = issued(8L, "other", new Date(System.currentTimeMillis() + 60_000L));
        when(mapper.selectByKeyForUpdate("other")).thenReturn(otherUser);

        assertThrows(ConflictException.class,
                () -> service.requireForUpdate(7L, "missing", "fp"));
        assertThrows(ConflictException.class,
                () -> service.requireForUpdate(7L, "other", "fp"));
    }

    @Test
    void unusedExpiredKeyIsRejected() {
        when(mapper.selectByKeyForUpdate("expired"))
                .thenReturn(issued(7L, "expired", new Date(System.currentTimeMillis() - 1_000L)));

        assertThrows(ConflictException.class,
                () -> service.requireForUpdate(7L, "expired", "fp"));
    }

    @Test
    void completedKeyIgnoresExpiryButRequiresTheSameFingerprint() {
        OrderIdempotency completed = issued(
                7L,
                "completed",
                new Date(System.currentTimeMillis() - 180_000L)
        );
        completed.setStatus("COMPLETED");
        completed.setRequestFingerprint("fp-1");
        completed.setOrderId(88L);
        when(mapper.selectByKeyForUpdate("completed")).thenReturn(completed);

        assertEquals(completed, service.requireForUpdate(7L, "completed", "fp-1"));
        assertThrows(ConflictException.class,
                () -> service.requireForUpdate(7L, "completed", "fp-2"));
    }

    @Test
    void issuedKeyCanBeCompletedExactlyOnce() {
        when(mapper.completeIssued(eq(5L), eq("fp-1"), eq(88L), any(Date.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.complete(5L, "fp-1", 88L));

        verify(mapper).completeIssued(eq(5L), eq("fp-1"), eq(88L), any(Date.class));
    }

    @Test
    void failedCompletionIsAConflict() {
        when(mapper.completeIssued(eq(5L), eq("fp-1"), eq(88L), any(Date.class))).thenReturn(0);

        assertThrows(ConflictException.class, () -> service.complete(5L, "fp-1", 88L));
    }

    @Test
    void expirationAndCleanupNeverTargetCompletedRows() {
        service.expireIssuedKeyIfNeeded(7L, "key-1");
        service.cleanupExpiredUnusedKeys();

        verify(mapper).expireIssuedKeyIfNeeded(eq(7L), eq("key-1"), any(Date.class));
        verify(mapper).deleteUnusedExpiredBefore(any(Date.class));
    }

    @Test
    void expirationUpdateCommitsIndependentlyBeforeTheOrderTransaction() throws Exception {
        Transactional transactional = OrderIdempotencyServiceImpl.class
                .getMethod("expireIssuedKeyIfNeeded", Long.class, String.class)
                .getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }

    private OrderIdempotency issued(Long userId, String key, Date expiresAt) {
        OrderIdempotency record = new OrderIdempotency();
        record.setId(5L);
        record.setUserId(userId);
        record.setIdempotencyKey(key);
        record.setStatus("ISSUED");
        record.setExpiresAt(expiresAt);
        return record;
    }
}

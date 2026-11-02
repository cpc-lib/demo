package com.example.fencing.service;

import com.example.fencing.error.IdempotencyConflictException;
import com.example.fencing.error.StaleFenceException;
import com.example.fencing.model.MutationResponse;
import com.example.fencing.model.OperationRecord;
import com.example.fencing.repository.OperationRepository;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounterMutationService {
    private final JdbcTemplate jdbc;
    private final OperationRepository operations;

    public CounterMutationService(JdbcTemplate jdbc, OperationRepository operations) {
        this.jdbc = jdbc;
        this.operations = operations;
    }

    /**
     * Returns the stored result for repeated requests; does not write anything.
     * Returns null when the idempotency key is new.
     */
    public MutationResponse replayIfExists(String resourceId, String requestId, long delta) {
        Optional<OperationRecord> existing = operations.find(resourceId, requestId);
        if (existing.isEmpty()) {
            return null;
        }
        if (existing.get().delta() != delta) {
            throw new IdempotencyConflictException(requestId);
        }
        return existing.get().toResponse(true);
    }

    /**
     * Atomic DB transaction: conditional write + idempotency record.
     * Every protected write path must carry an equality check on fence_token.
     */
    @Transactional
    public MutationResponse apply(String resourceId, String requestId, long delta, long token) {
        // Check again inside the transaction in case another lock owner completed while waiting.
        MutationResponse prior = replayIfExists(resourceId, requestId, delta);
        if (prior != null) {
            return prior;
        }

        int changed = jdbc.update("""
                UPDATE fenced_counter
                SET counter_value = counter_value + ?
                WHERE resource_id = ? AND fence_token = ?
                """, delta, resourceId, token);
        if (changed != 1) {
            throw new StaleFenceException(resourceId, token);
        }
        Long value = jdbc.queryForObject("""
                SELECT counter_value FROM fenced_counter WHERE resource_id = ?
                """, Long.class, resourceId);
        operations.insert(resourceId, requestId, delta, value, token);
        return new MutationResponse(resourceId, requestId, delta, value, token, false);
    }
}

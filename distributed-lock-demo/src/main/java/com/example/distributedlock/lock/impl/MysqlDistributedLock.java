package com.example.distributedlock.lock.impl;

import com.example.distributedlock.lock.DistributedLock;
import com.example.distributedlock.lock.LockHandle;
import com.example.distributedlock.lock.LockType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MysqlDistributedLock implements DistributedLock {

    private final JdbcTemplate jdbcTemplate;
    private final long retryIntervalMs;
    private final long defaultLeaseMs;
    private final String processId = UUID.randomUUID().toString();

    public MysqlDistributedLock(
            JdbcTemplate jdbcTemplate,
            @Value("${app.lock.mysql.retry-interval-ms:50}") long retryIntervalMs,
            @Value("${app.lock.mysql.default-lease-ms:10000}") long defaultLeaseMs) {
        this.jdbcTemplate = jdbcTemplate;
        this.retryIntervalMs = retryIntervalMs;
        this.defaultLeaseMs = defaultLeaseMs;
    }

    @Override
    public LockType type() {
        return LockType.MYSQL;
    }

    @Override
    public Optional<LockHandle> tryLock(String key, Duration waitTime, Duration leaseTime) {
        long leaseMs = leaseTime == null || leaseTime.isZero() || leaseTime.isNegative()
                ? defaultLeaseMs
                : leaseTime.toMillis();
        String owner = processId + ":" + Thread.currentThread().threadId() + ":" + UUID.randomUUID();
        long deadline = System.nanoTime() + waitTime.toNanos();

        do {
            if (tryAcquire(key, owner, leaseMs)) {
                AtomicBoolean closed = new AtomicBoolean(false);
                return Optional.of(new LockHandle() {
                    @Override
                    public String key() {
                        return key;
                    }

                    @Override
                    public LockType type() {
                        return LockType.MYSQL;
                    }

                    @Override
                    public void close() {
                        if (closed.compareAndSet(false, true)) {
                            jdbcTemplate.update(
                                    "DELETE FROM distributed_lock WHERE lock_key = ? AND owner = ?",
                                    key,
                                    owner
                            );
                        }
                    }
                });
            }

            if (System.nanoTime() >= deadline) {
                break;
            }

            try {
                Thread.sleep(Math.max(1, retryIntervalMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        } while (true);

        return Optional.empty();
    }

    /**
     * 用单条 INSERT ... ON DUPLICATE KEY UPDATE 原子完成"插入新锁 / 接管过期锁"。
     * 不能拆成 INSERT + UPDATE 两条语句：并发下多个事务先因唯一键冲突持 S 锁、
     * 再各自请求 X 锁，会互相等待形成死锁（error 1213）。
     *
     * 返回行数语义（依赖 JDBC URL 上的 useAffectedRows=true）：
     * 1 = 新插入获得锁；2 = 接管过期锁；0 = 锁被持有且未过期。
     */
    private boolean tryAcquire(String key, String owner, long leaseMs) {
        try {
            return jdbcTemplate.update("""
                    INSERT INTO distributed_lock(lock_key, owner, expire_at)
                    VALUES (?, ?, TIMESTAMPADD(MICROSECOND, ? * 1000, CURRENT_TIMESTAMP(6)))
                    ON DUPLICATE KEY UPDATE
                        owner     = IF(expire_at < CURRENT_TIMESTAMP(6), VALUES(owner), owner),
                        expire_at = IF(expire_at < CURRENT_TIMESTAMP(6), VALUES(expire_at), expire_at)
                    """, key, owner, leaseMs) > 0;
        } catch (DuplicateKeyException | TransientDataAccessException ignored) {
            // 死锁/锁等待超时等瞬态异常：MySQL 已回滚当前语句，视为本轮抢锁失败，由外层循环重试
            return false;
        }
    }
}

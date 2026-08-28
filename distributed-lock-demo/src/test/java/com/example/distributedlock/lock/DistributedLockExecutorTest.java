package com.example.distributedlock.lock;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DistributedLockExecutorTest {

    @Test
    void shouldExecuteCriticalSectionAndReleaseLock() {
        AtomicBoolean released = new AtomicBoolean(false);
        DistributedLock fake = successfulFake(released);
        DistributedLockExecutor executor = new DistributedLockExecutor(
                new DistributedLockRegistry(List.of(fake))
        );

        String result = executor.execute(
                LockType.REDISSON,
                "inventory:1001",
                Duration.ofSeconds(1),
                Duration.ofSeconds(10),
                () -> "OK"
        );

        assertThat(result).isEqualTo("OK");
        assertThat(released).isTrue();
    }

    @Test
    void shouldFailWhenLockCannotBeAcquired() {
        DistributedLock fake = new DistributedLock() {
            @Override
            public LockType type() {
                return LockType.REDISSON;
            }

            @Override
            public Optional<LockHandle> tryLock(String key, Duration waitTime, Duration leaseTime) {
                return Optional.empty();
            }
        };

        DistributedLockExecutor executor = new DistributedLockExecutor(
                new DistributedLockRegistry(List.of(fake))
        );

        assertThatThrownBy(() -> executor.execute(
                LockType.REDISSON,
                "inventory:1001",
                Duration.ofMillis(1),
                Duration.ofSeconds(1),
                () -> "never"
        )).isInstanceOf(DistributedLockExecutor.LockAcquisitionException.class);
    }

    private DistributedLock successfulFake(AtomicBoolean released) {
        return new DistributedLock() {
            @Override
            public LockType type() {
                return LockType.REDISSON;
            }

            @Override
            public Optional<LockHandle> tryLock(String key, Duration waitTime, Duration leaseTime) {
                return Optional.of(new LockHandle() {
                    @Override
                    public String key() {
                        return key;
                    }

                    @Override
                    public LockType type() {
                        return LockType.REDISSON;
                    }

                    @Override
                    public void close() {
                        released.set(true);
                    }
                });
            }
        };
    }
}

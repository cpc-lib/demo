package cc.ivera.support;

import cc.ivera.exception.BizException;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class DistributedLockExecutor implements DisposableBean {

    private final String host;

    private final int port;

    private final String password;

    private final int database;

    private final boolean ssl;

    private final long waitTimeSeconds;

    private volatile RedissonClient redissonClient;

    public DistributedLockExecutor(@Value("${spring.redis.host:127.0.0.1}") String host,
                                   @Value("${spring.redis.port:6379}") int port,
                                   @Value("${spring.redis.password:}") String password,
                                   @Value("${spring.redis.database:0}") int database,
                                   @Value("${spring.redis.ssl:false}") boolean ssl,
                                   @Value("${payment.lock.wait-time-seconds:10}") long waitTimeSeconds) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.database = database;
        this.ssl = ssl;
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public void execute(String lockKey, Runnable action) {
        execute(lockKey, () -> {
            action.run();
            return null;
        });
    }

    public <T> T execute(String lockKey, Supplier<T> action) {
        RLock lock = getRedissonClient().getLock(lockKey);
        boolean locked = false;
        try {
            // Do not pass leaseTime: Redisson watchdog keeps renewing while the thread is alive.
            locked = lock.tryLock(waitTimeSeconds, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException("系统繁忙，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("获取分布式锁被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void destroy() {
        RedissonClient client = redissonClient;
        if (client != null && !client.isShutdown()) {
            client.shutdown();
        }
    }

    private RedissonClient getRedissonClient() {
        RedissonClient client = redissonClient;
        if (client == null) {
            synchronized (this) {
                client = redissonClient;
                if (client == null) {
                    client = Redisson.create(buildConfig());
                    redissonClient = client;
                }
            }
        }
        return client;
    }

    private Config buildConfig() {
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress((ssl ? "rediss://" : "redis://") + host + ":" + port)
                .setDatabase(database);
        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }
        return config;
    }
}

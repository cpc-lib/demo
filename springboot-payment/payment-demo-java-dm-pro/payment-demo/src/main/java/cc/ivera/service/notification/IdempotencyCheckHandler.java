package cc.ivera.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 通知幂等检查处理器 — 通过 Redis SETNX 防止重复处理。
 */
@Slf4j
public class IdempotencyCheckHandler implements NotificationHandler {

    private static final String IDEMPOTENT_KEY_PREFIX = "payment:notify:idempotent:";
    private static final long IDEMPOTENT_EXPIRE_HOURS = 24L;

    private final StringRedisTemplate stringRedisTemplate;

    public IdempotencyCheckHandler(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void handle(NotificationContext context, NotificationChain chain) {
        String notifyId = context.getNotifyId();
        if (notifyId == null || notifyId.trim().isEmpty()) {
            log.warn("通知ID为空，跳过幂等检查");
            chain.proceed(context);
            return;
        }

        String key = IDEMPOTENT_KEY_PREFIX + notifyId;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", IDEMPOTENT_EXPIRE_HOURS, TimeUnit.HOURS);

        if (Boolean.TRUE.equals(acquired)) {
            log.info("通知幂等检查通过，notifyId={}", notifyId);
            chain.proceed(context);
        } else {
            log.info("通知已处理或正在处理中，幂等忽略，notifyId={}", notifyId);
            context.setProcessed(true);
        }
    }
}

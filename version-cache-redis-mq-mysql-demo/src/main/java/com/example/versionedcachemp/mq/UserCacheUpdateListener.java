package com.example.versionedcachemp.mq;

import com.example.versionedcachemp.config.MqConfig;
import com.example.versionedcachemp.domain.UserAccount;
import com.example.versionedcachemp.mapper.UserAccountMapper;
import com.example.versionedcachemp.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * MQ 消费者：收到用户更新事件后，从 DB 读取最新数据，
 * 然后用 Versioned Cache 逻辑刷新 Redis。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCacheUpdateListener {

    private final UserAccountMapper userMapper;
    private final UserAccountService userService;

    @RabbitListener(queues = MqConfig.QUEUE_USER_CACHE_UPDATE)
    @Transactional(readOnly = true)
    public void onUserUpdated(UserUpdatedEvent event) {
        log.info("[MQ RECEIVED] user updated event: id={}, version={}",
                event.getUserId(), event.getVersion());

        UserAccount user = userMapper.selectById(event.getUserId());
        if (user == null) {
            log.warn("[MQ IGNORE] user not found, id={}", event.getUserId());
            return;
        }

        // 使用实际 DB 中的 version（可能比 event.version 更大）
        userService.writeCacheWithVersionCheck(user);
    }
}

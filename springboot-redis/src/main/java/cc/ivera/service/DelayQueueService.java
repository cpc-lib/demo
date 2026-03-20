package cc.ivera.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;


@Slf4j
@Service
public class DelayQueueService {
    private static final String DELAY_QUEUE_KEY = "delay_queue";
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void addToDelayQueue(String message, long delayTime) {
        redisTemplate.opsForZSet().add(DELAY_QUEUE_KEY, message, System.currentTimeMillis() + delayTime);
    }

    public void pollAndProcessDelayedMessages() {
        Set<String> messages = redisTemplate.opsForZSet().rangeByScore(DELAY_QUEUE_KEY, 0, System.currentTimeMillis());
        for (String message : messages) {
            // 处理消息
            processMessage(message);
            // 从延迟队列中删除已处理的消息
            redisTemplate.opsForZSet().remove(DELAY_QUEUE_KEY, message);
        }
    }

    private void processMessage(String message) {
        // 根据业务需求进行消息处理
        log.info("Processing message: " + message);
    }
}
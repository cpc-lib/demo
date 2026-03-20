package cc.ivera.timer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import cc.ivera.service.DelayQueueService;

@Component
public class DelayQueueSchedule {
    @Autowired
    private DelayQueueService delayQueueService;

    // 每隔一段时间进行轮询并处理延迟消息
    @Scheduled(fixedDelay = 1000)
    public void pollAndProcessDelayedMessages() {
        delayQueueService.pollAndProcessDelayedMessages();
    }
}
package cc.ivera.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.redis.domain.DelayMessage;
import cc.ivera.redis.domain.DelayQueue;

@RestController
public class RedisDelayQueueController {
    @Autowired
    private DelayQueue delayQueue;

    @GetMapping("/redis/add")
    public void addTask(@RequestParam("task") String task) {
        //获取当前系统时间
        long newDate = System.currentTimeMillis();
        // 添加消息：1秒后到期
        DelayMessage message = new DelayMessage("1", "task", newDate + 1000);
        delayQueue.put(message);
    }

}

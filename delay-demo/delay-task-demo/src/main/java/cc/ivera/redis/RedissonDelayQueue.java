package cc.ivera.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


/**
 * 微信公众号：三友的java日记
 *
 * @author sanyou
 * @date 2023/2/12 17:53
 */
@Component
@Slf4j
public class RedissonDelayQueue {

    private RedissonClient redissonClient;

    private RDelayedQueue<String> delayQueue;
    private RBlockingQueue<String> blockingQueue;

    @PostConstruct
    public void init() {
        initDelayQueue();
        startDelayQueueConsumer();
    }

    private void initDelayQueue() {
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer();
        serverConfig.setAddress("redis://114.132.210.77:6380");
        serverConfig.setPassword("cpc!23#@");
        serverConfig.setDatabase(0);
        serverConfig.setConnectionPoolSize(64);
        serverConfig.setConnectionMinimumIdleSize(24);
        redissonClient = Redisson.create(config);
        blockingQueue = redissonClient.getBlockingQueue("SANYOU");
        delayQueue = redissonClient.getDelayedQueue(blockingQueue);
    }

    private void startDelayQueueConsumer() {
        new Thread(() -> {
            while (true) {
                try {
                    String task = blockingQueue.take();
                    log.info("接收到延迟任务:{}", task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "SANYOU-Consumer").start();
    }

    public void offerTask(String task, long seconds) {
        log.info("添加延迟任务:{} 延迟时间:{}s", task, seconds);
        delayQueue.offer(task, seconds, TimeUnit.SECONDS);
    }

}

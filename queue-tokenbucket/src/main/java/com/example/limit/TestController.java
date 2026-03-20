package com.example.limit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例 Controller：
 *
 * 暴露一个 /work 接口用于压测和演示限流效果。
 */
@RestController
public class TestController {

    /**
     * 示例业务接口：
     *  - 使用 @QueueTokenLimit 进行限流控制
     */
    @QueueTokenLimit(
            key = "demo:work",      // 限流 key，同一集群所有实例共享
            maxConcurrency = 3,     // 最大并发 = 3
            enableQueue = true,     // 打开排队等待
            queueTimeoutMs = 500,  // 排队最多等待 0.5 秒
            tokenRate = 10,         // 全局 QPS 上限约 10
            tokenBucketSize = 10,   // 这里只是语义字段，如需自定义可扩展
            message = "系统繁忙，请稍后重试"
    )
    @GetMapping("/work")
    public String doWork() throws InterruptedException {
        return "OK - " + System.currentTimeMillis();
    }
}

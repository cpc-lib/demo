package cc.ivera.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.annotation.RateLimiter;
import cc.ivera.enums.LimitType;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shamee
 * 限流测试
 */
@RestController
@RequestMapping("/api")
public class LimitController {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger();

    /**
     * 测试限流注解，
     * 60秒内最多只能访问 5次，保存到redis的键名为 limitP_testK，
     */
    @GetMapping("/test")
    public String test() {
        return "hello>>>" + new Date();
    }


    @GetMapping("/test1")
    @RateLimiter(time = 5, count = 3, limitType = LimitType.IP)
    public String test1() {
        return "hello>>>" + new Date();
    }
}
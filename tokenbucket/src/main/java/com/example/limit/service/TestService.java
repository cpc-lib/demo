package com.example.limit.service;

import com.example.limit.annotation.TokenBucketLimit;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TestService {


    /**
     * rate含义是:
     * 每秒最多允许 5 次调用
     * 令牌桶每隔 200ms 补充 1 个令牌（1000ms/5 = 200ms
     * QPS 精准控制在 5
     *
     * capacity含义是：
     * 最多能积累 10 个令牌
     * 如果接口长时间不调用，就可能积攒完整 10 个令牌
     * 一瞬间（突发）可以连着调用 10 次
     * 超出后按 rate 限制节奏补
     * 👉 用于 平滑突发请求。
     */
    @TokenBucketLimit(key = "api", rate = 3, capacity = 6)
    public Map<String,Object> call() {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);  // HTTP 429: Too Many Requests
        res.put("msg", "OK");
        return res;
    }
}

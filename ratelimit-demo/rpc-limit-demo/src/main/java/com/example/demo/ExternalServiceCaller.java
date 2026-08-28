package com.example.demo;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ExternalServiceCaller {
    private final DistributedSemaphoreLimiter limiter;
    private final ExternalRpcClient rpcClient;

    public ExternalServiceCaller(DistributedSemaphoreLimiter limiter,
                                 ExternalRpcClient rpcClient) {
        this.limiter = limiter;
        this.rpcClient = rpcClient;
    }

    public Map<String, Object> call() {
        Map<String, Object> map = new HashMap<>();
        if (!limiter.tryAcquire()) {
            map.put("code", -1);
            map.put("message", "失败");
            return map;
        }
        try {
            map.put("code", 0);
            map.put("message", rpcClient.callExternalApi());
            return map;
        } finally {
            limiter.release();
        }
    }
}

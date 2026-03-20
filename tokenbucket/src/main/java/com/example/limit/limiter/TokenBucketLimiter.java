package com.example.limit.limiter;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.util.Arrays;

public class TokenBucketLimiter {
    private final RedissonClient client;
    private final String script;

    public TokenBucketLimiter(RedissonClient c) {
        this.client = c;
        this.script = LuaScriptLoader.load("lua/token_bucket.lua");
        System.out.println("Loaded Lua:\n" + this.script);
    }

    public boolean tryAcquire(String key, long rate, long cap) {
        String tk = "tb:" + key + ":tokens";
        String ts = "tb:" + key + ":ts";
        Long r = rate, c = cap;
        String rateStr = r.toString();
        String capStr  = c.toString();
        Long res = client.getScript().eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.INTEGER,
                Arrays.asList(tk, ts),
                rateStr, capStr
        );
        return res != null && res == 1L;
    }
}

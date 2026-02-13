package cc.ivera.ratelimit.core.client;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RedisScriptRegistry {

    private final Map<RateLimitStrategy, DefaultRedisScript<List>> scripts =
            new EnumMap<RateLimitStrategy, DefaultRedisScript<List>>(RateLimitStrategy.class);

    public RedisScriptRegistry() {
        scripts.put(RateLimitStrategy.FIXED_WINDOW, load("ratelimit-lua/fixed_window.lua"));
        scripts.put(RateLimitStrategy.SLIDING_WINDOW, load("ratelimit-lua/sliding_window.lua"));
        scripts.put(RateLimitStrategy.TOKEN_BUCKET, load("ratelimit-lua/token_bucket.lua"));
    }

    public DefaultRedisScript<List> script(RateLimitStrategy s) {
        return scripts.get(s);
    }

    private DefaultRedisScript<List> load(String path) {
        DefaultRedisScript<List> script = new DefaultRedisScript<List>();
        script.setResultType(List.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        return script;
    }
}

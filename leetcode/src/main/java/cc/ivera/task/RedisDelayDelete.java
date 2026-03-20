package cc.ivera.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cc.ivera.util.RedisUtil;

@Component("redisDelayDelete")
public class RedisDelayDelete {
    @Autowired
    private RedisUtil redisUtil;

    public void execute(String keyId) {
        redisUtil.remove(keyId);
    }

}

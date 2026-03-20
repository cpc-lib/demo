package cc.ivera;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import cc.ivera.domain.DelayMessage;
import cc.ivera.domain.DelayQueue;

@SpringBootTest
public class ApplicationTest {

    /***
     * 使用RedisTemplate操作key，redis服务内部进行了序列化处理与使用redis-cli设置的key不同。
     * 使用命令flushall可以删除redis中的所有key
     */
    @Autowired
    private RedisTemplate redisTemplate;

    /***
     * 使用StringRedisTemplate操作key与redis-cli操作key的逻辑相同。
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /***
     * jedis与lettuce区别
     * jedis连接Redis服务器是直连模式，当多线程模式下使用jedis会存在线程安全问题，解决方案可以通过配置连接池使每个连接专用，这样整体性能就大受影响。
     * lettuce基于Netty框架进行与Redis服务器连接，底层设计中采用StatefulRedisConnection。 StatefulRedisConnection自身是线程安全的，可以保障并发访问安全问题，所以一个连接可以被多线程复用。当然lettcus也支持多连接实例一起工作
     */
    @Test
    public void set() {
        ValueOperations ops = redisTemplate.opsForValue();
        ops.set("name", "张三");
    }

    @Test
    public void get() {
        ValueOperations ops = redisTemplate.opsForValue();
        Object name = ops.get("name");
        if (name != null && name instanceof String) {
            System.out.println((String) name);
        }

    }

    @Test
    public void set2() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("name2", "hejiaming");
    }

    @Test
    public void get2() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        System.out.println(ops.get("name2"));
    }

    @Test
    public void hset() {
        HashOperations<String, Object, Object> ops = stringRedisTemplate.opsForHash();
        ops.put("personInfo", "name", "hejiaming");
        //value的类型以String保存到数据库，避免出错
        ops.put("personInfo", "age", "25");
    }

    @Test
    public void hget() {
        HashOperations<String, Object, Object> ops = stringRedisTemplate.opsForHash();
        Object name = ops.get("personInfo", "name");
        //instanceof用于判断对象的类型
        if (name instanceof String) {
            System.out.println((String) name);
        }
        Object age = ops.get("personInfo", "age");
        if (age instanceof String) {
            System.out.println((String) age);
        }
    }


    @Autowired
    private DelayQueue delayQueue;

    @org.junit.Test
    public void testDelayQueue() {
        //获取当前系统时间
        long newDate = System.currentTimeMillis();

        // 添加消息：1秒后到期
        DelayMessage message1 = new DelayMessage("1", "delay message 1", newDate + 1000);
        // 添加消息：3秒后到期
        DelayMessage message2 = new DelayMessage("2", "delay message 2", newDate + 3000);
        // 添加消息：6秒后到期
        DelayMessage message3 = new DelayMessage("3", "delay message 3", newDate + 6000);
        // 添加消息：10秒后到期
        DelayMessage message4 = new DelayMessage("4", "delay message 4", newDate + 10000);

        delayQueue.put(message1);//1s后执行
        delayQueue.put(message2);//3s后执行
        delayQueue.put(message3);//6s后执行
        delayQueue.put(message4);//10s后执行
    }

}

package cc.ivera.controller;

import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;


@RestController
public class RedissionController {

    Logger log = LoggerFactory.getLogger(RedissionController.class);

    @Autowired
    private RedissonClient redissonClient;

    @GetMapping("/lock")
    public String lock() throws InterruptedException {
        String lockKey = "lock";
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockResult = lock.tryLock();
        log.info("lock key {} lock result {}", lockKey, lockResult);
        TimeUnit.SECONDS.sleep(5);
        lock.unlock();
        log.info("lock key {} unlock ", lockKey);
        return System.currentTimeMillis() + "---" + lockResult;
    }

    @GetMapping("/get/{key}")
    public String hi(@PathVariable String key) {
        //获取key的值
        RBucket<String> bucket = redissonClient.getBucket(key);
        String value = bucket.get();
        return value;
    }

    @GetMapping("/set/{key}/{value}")
    public String hi(@PathVariable String key, @PathVariable String value) {
        //设置key的value
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value);
        return "success";
    }

    @PostMapping("/op")
    public String op() {
        // 2. 操作String类型
        RBucket<String> bucket = redissonClient.getBucket("myString");
        bucket.set("Hello, Redisson");
        String value = bucket.get();
        System.out.println("String Value: " + value);

        // 3. 操作List类型
        RList<String> list = redissonClient.getList("myList");
        list.add("A");
        list.add("B");
        list.add("C");
        System.out.println("List Values: " + list);

        // 4. 操作Set类型
        RSet<String> set = redissonClient.getSet("mySet");
        set.add("X");
        set.add("Y");
        set.add("Z");
        System.out.println("Set Values: " + set);

        // 5. 操作Hash类型
        RMap<String, String> map = redissonClient.getMap("myMap");
        map.put("key1", "value1");
        map.put("key2", "value2");
        System.out.println("Map Values: " + map);

        // 6. 操作Sorted Set（ZSet）类型
        RSortedSet<String> sortedSet = redissonClient.getSortedSet("myZSet");
        sortedSet.add("Element1");
        sortedSet.add("Element2");
        sortedSet.add("Element3");
        System.out.println("SortedSet Values: " + sortedSet);
        return "success";
    }


}
 
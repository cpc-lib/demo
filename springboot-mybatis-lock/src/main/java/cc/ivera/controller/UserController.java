package cc.ivera.controller;

import com.github.pagehelper.PageHelper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import cc.ivera.domain.User;
import cc.ivera.service.User2Service;
import cc.ivera.util.RedissonService;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
@Scope("singleton")
@RequestMapping("/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private User2Service userService;

    @Autowired
    private RedissonService redissonService;

    @Autowired
    private CuratorFramework curatorFramework;


    @PostMapping
    public boolean save(@RequestBody User user) {
        return userService.save(user);
    }

    @DeleteMapping("{id}")
    public boolean delete(@PathVariable Integer id) {
        return userService.delete(id);
    }

    @PutMapping
    public boolean update(@RequestBody User user) {
        return userService.update(user);
    }

    @GetMapping("{page}/{size}")
    public List<User> findAll(@PathVariable Integer page,
                              @PathVariable Integer size) {
        //分页拦截，此处执行语句count与limit控制语句(MyBatis Log的使用)
        PageHelper.startPage(page, size);
        List<User> userList = userService.findAll();
        return userList;
    }

    @GetMapping("{id}")
    public User findById(@PathVariable Integer id) {
        return userService.findById(id);
    }


    @GetMapping("/createUser0")
    public User createUser0() {
        User byId = userService.findById(100);
        Random random = new Random();
        int i = random.nextInt(100);
        if (byId == null) {
            byId = new User();
            i = i + 101;
            byId.setId(i);
            userService.save(byId);
        }
        User user = new User();
        user.setId(100);
        userService.save(user);
        System.out.println(user);
        return user;
    }


    /**
     * 使用synchronized加锁避免出现脏数据
     * 锁对象为this
     * 如果配置scope为prototype此时任务出现脏数据
     * 单机部署生效
     * @return
     */
    @GetMapping("/createUser1")
    public synchronized User createUser1() {
        User byId = userService.findById(100);
        Random random = new Random();
        int i = random.nextInt(100);
        if (byId == null) {
            byId = new User();
            i = i + 101;
            byId.setId(i);
            userService.save(byId);
        }
        User user = new User();
        user.setId(100);
        userService.save(user);
        System.out.println(user);
        return user;
    }


    /**
     * 测试使用redisson的控制
     * 分布式锁
     */
    @RequestMapping(value = "/createUser2")
    public User createUser2() {
        String lockKey = "lock-key";
        RLock lock = redissonService.getRLock(lockKey);
        try {
            boolean bs = lock.tryLock(5, 6, TimeUnit.SECONDS);
            if (bs) {
                User byId = userService.findById(100);
                Random random = new Random();
                int i = random.nextInt(100);
                if (byId == null) {
                    byId = new User();
                    i = i + 101;
                    byId.setId(i);
                    userService.save(byId);
                }
                User user = new User();
                user.setId(100);
                userService.save(user);
                System.out.println(user);
                lock.unlock();
            } else {

            }
        } catch (Exception e) {
            log.error("", e);
            lock.unlock();
        }
        return new User();
    }


    @RequestMapping(value = "/createUser/demo")
    public User createUserDemo(String recordId) {
        RLock lock = redissonService.getRLock(recordId);
        try {
            boolean bs = lock.tryLock(5, 6, TimeUnit.SECONDS);
            if (bs) {
                // 业务代码
                log.info("进入业务代码: " + recordId);

                lock.unlock();
            } else {
                Thread.sleep(300);
            }
        } catch (Exception e) {
            log.error("", e);
            lock.unlock();
        }
        return new User();
    }


    /**
     * 分布式锁zk的客户端curator
     * @param recordId
     * @return
     */
    @RequestMapping("/createUser3")
    public void createUser3() {
        InterProcessSemaphoreMutex mutex = new InterProcessSemaphoreMutex(curatorFramework, "/curator/lock");
        try {
            mutex.acquire();

            User byId = userService.findById(100);
            Random random = new Random();
            int i = random.nextInt(100);
            if (byId == null) {
                byId = new User();
                i = i + 101;
                byId.setId(i);
                userService.save(byId);
            }
            User user = new User();
            user.setId(100);
            userService.save(user);
            System.out.println(user);

            // 处理业务
            // 例如查询库存 扣减库存


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mutex.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}

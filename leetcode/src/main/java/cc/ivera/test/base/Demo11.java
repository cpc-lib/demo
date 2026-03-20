package cc.ivera.test.base;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import cc.ivera.mapper.OrderMapper;
import cc.ivera.mapper.UserMapper;
import cc.ivera.model.pojo.Order;
import cc.ivera.model.pojo.User;
import cc.ivera.service.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * mybatisplus的批量插入
 */
@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class Demo11 {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;


    @Autowired
    private OrderMapper orderMapper;

    @Test
    public void testInsertBatch() {
        //604ms
        long start = System.currentTimeMillis();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            User user = new User();
            user.setName("犬小哈" + i);
            user.setAge(i);
            user.setGender(1);
            users.add(user);
        }

        userMapper.insertBatchSomeColumn(users);
        System.out.println(String.format("总耗时：%s ms", System.currentTimeMillis() - start));
    }

    @Test
    public void testInsert1() {
        // 总耗时：182391 ms
        long start = System.currentTimeMillis();
        for (int i = 0; i < 3000; i++) {
            User user = new User();
            user.setName("犬小哈" + i);
            user.setAge(i);
            user.setGender(1);
            userMapper.insert(user);
        }
        System.out.println(String.format("总耗时：%s ms", System.currentTimeMillis() - start));
    }


    @Test
    public void testInsert2() {
        // 总耗时：83668 ms
        long start = System.currentTimeMillis();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            User user = new User();
            user.setName("犬小哈" + i);
            user.setAge(i);
            user.setGender(1);
            users.add(user);
        }
        userService.saveBatch(users);
        System.out.println(String.format("总耗时：%s ms", System.currentTimeMillis() - start));
    }


    @Test
    public void testInsertBatch1() {
        // 总耗时：656 ms
        long start = System.currentTimeMillis();
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            User user = new User();
            user.setName("犬小哈" + i);
            user.setAge(i);
            user.setGender(1);
            users.add(user);
        }

        // 分片插入（每 1000 条执行一次批量插入）
        int batchSize = 1000;
        int total = users.size();
        // 需要执行的次数
        int insertTimes = total / batchSize;
        // 最后一次执行需要提交的记录数（防止可能不足 1000 条）
        int lastSize = batchSize;
        if (total % batchSize != 0) {
            insertTimes++;
            lastSize = total % batchSize;
        }

        for (int j = 0; j < insertTimes; j++) {
            if (insertTimes == j + 1) {
                batchSize = lastSize;
            }

            // 分片执行批量插入
            userMapper.insertBatchSomeColumn(users.subList(j * batchSize, (j * batchSize + batchSize)));
        }
        System.out.println(String.format("总耗时：%s ms", System.currentTimeMillis() - start));
    }


    @Test
    public void testInsertByXml() {
        Order order = new Order(2l, 2l);
        order.setId(null);
        orderMapper.save(order);

    }

}

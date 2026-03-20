//package cc.ivera.test;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import cc.ivera.model.pojo.DelayMessage;
//import cc.ivera.model.pojo.DelayQueue;
//
//
///**
// * redis-zset 延时队列
// */
//@SpringBootTest
//@RunWith(SpringJUnit4ClassRunner.class)
//public class Demo12 {
//
//    @Autowired
//    private DelayQueue delayQueue;
//
//    @Test
//    public void testDelayQueue() {
//        //获取当前系统时间
//        long newDate = System.currentTimeMillis();
//
//        // 添加消息：1秒后到期
//        DelayMessage message1 = new DelayMessage("1", "delay message 1", newDate + 3000);
//        // 添加消息：3秒后到期
//        DelayMessage message2 = new DelayMessage("2", "delay message 2", newDate + 30000);
//        // 添加消息：6秒后到期
//        DelayMessage message3 = new DelayMessage("3", "delay message 3", newDate + 60000);
//        // 添加消息：10秒后到期
//        DelayMessage message4 = new DelayMessage("4", "delay message 4", newDate + 100000);
//
//        delayQueue.put(message1);//1s后执行
//        delayQueue.put(message2);//3s后执行
//        delayQueue.put(message3);//6s后执行
//        delayQueue.put(message4);//10s后执行
//    }
//}

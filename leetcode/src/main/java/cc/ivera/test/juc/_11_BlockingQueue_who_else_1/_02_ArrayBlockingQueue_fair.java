package cc.ivera.test.juc._11_BlockingQueue_who_else_1;


import cc.ivera.test.juc.SmallTool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class _02_ArrayBlockingQueue_fair {
    private static BlockingQueue<String> shaobingQueue = new ArrayBlockingQueue<>(3, true);

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                SmallTool.printTimeAndThread("来买烧饼");
                String shaobing = shaobingQueue.poll(1, TimeUnit.SECONDS);
                String tag = shaobing == null ? "再见, 以后不来了" : "买到烧饼了";
                SmallTool.printTimeAndThread(tag);
            } catch (InterruptedException e) {
                SmallTool.printTimeAndThread("被中断" + e.getMessage());
            }
        }, "张三").start();

        SmallTool.sleepMillis(100);     // 模拟张三先到

        new Thread(() -> {
            try {
                SmallTool.printTimeAndThread("来买烧饼");
                String shaobing = shaobingQueue.poll(1, TimeUnit.SECONDS);
                String tag = shaobing == null ? "草, 不敢掀昊天宗的摊子! " : "买到烧饼了";
                SmallTool.printTimeAndThread(tag);
            } catch (InterruptedException e) {
                SmallTool.printTimeAndThread("被中断" + e.getMessage());
            }
        }, "李四").start();

        shaobingQueue.offer("芝麻烧饼");
    }
}

package cc.ivera.test.juc._11_BlockingQueue_who_else_1;


import cc.ivera.test.juc.SmallTool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class _04_SynchronousQueue {
    public static void main(String[] args) {
        BlockingQueue<String> shaobingQueue = new SynchronousQueue<>();

        new Thread(() -> {
            try {
                SmallTool.printTimeAndThread("准备开始做烧饼");
                shaobingQueue.put("芝麻烧饼1号");
                SmallTool.printTimeAndThread("卖出去了第1个烧饼");

                SmallTool.sleepMillis(2000);    // 休息两秒钟 再继续做

                shaobingQueue.put("芝麻烧饼2号");
                SmallTool.printTimeAndThread("卖出去了第2个烧饼");
            } catch (InterruptedException e) {
                SmallTool.printTimeAndThread("被中断" + e.getMessage());
            }
        }, "小白").start();

        new Thread(() -> {
            try {
                SmallTool.sleepMillis(1000);    // 还不饿，先等一秒

                SmallTool.printTimeAndThread("买到了" + shaobingQueue.take());
                SmallTool.printTimeAndThread("瞬间吃完，继续买");
                SmallTool.printTimeAndThread("买到了" + shaobingQueue.take());
            } catch (InterruptedException e) {
                SmallTool.printTimeAndThread("被中断" + e.getMessage());
            }
        }, "张三").start();

    }
}

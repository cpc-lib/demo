package cc.ivera.test.juc._10_BlockingQueue_basic;


import cc.ivera.test.juc.SmallTool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class _02_BlockingQueue_poll {
    public static void main(String[] args) {
        BlockingQueue<String> shaobingQueue = new LinkedBlockingQueue<>(3);

        Thread xiaoBai = new Thread(() -> {
            SmallTool.printTimeAndThread("小白 收拾东西，准备开张");
            SmallTool.printTimeAndThread("小白 接到电话 往家里跑");

        });

        Thread roadPeopleA = new Thread(() -> {
            SmallTool.printTimeAndThread("路人甲 来买烧饼");
            try {
                String shaobing = shaobingQueue.poll(2, TimeUnit.SECONDS);
                SmallTool.printTimeAndThread("路人甲 买到了烧饼: " + shaobing);
            } catch (InterruptedException e) {
                SmallTool.printTimeAndThread("路人甲 被中断" + e.getMessage());
            }
        });

        xiaoBai.start();
        try {
            Thread.sleep(10);   // 先等小白收拾一下，再让路人甲出场
        } catch (InterruptedException e) {
            SmallTool.printTimeAndThread("主线程 被中断" + e.getMessage());
        }
        roadPeopleA.start();
    }
}

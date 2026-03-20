package cc.ivera.test.juc._12_BlockingQueue_who_else_2;


import cc.ivera.test.juc.SmallTool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class _03_PriorityBlockingQueue_Pancake_Comparable {

    public static void main(String[] args) {
        BlockingQueue<Pancake> blockingQueue = new PriorityBlockingQueue<>();

        Thread xiaobai = new Thread(() -> {
            blockingQueue.offer(new Pancake(2));
            SmallTool.printTimeAndThread("做好第1个烧饼");

            blockingQueue.offer(new Pancake(0));
            SmallTool.printTimeAndThread("做好第2个烧饼");

            blockingQueue.offer(new Pancake(1));
            SmallTool.printTimeAndThread("做好第3个烧饼");
        }, "小白");

        xiaobai.start();
        try {
            xiaobai.join();     // 让小白顺利做完 3个烧饼
        } catch (InterruptedException e) {
            SmallTool.printTimeAndThread("被中断" + e.getMessage());
        }

        new Thread(() -> {
            SmallTool.printTimeAndThread("买到烧饼" + blockingQueue.poll());
        }, "张三").start();

    }

    private record Pancake(
            /**
             * 美味程度
             *  0 --> 好吃
             *  1 --> 一般
             *  2 --> 难吃
             */
            Integer flavor
    ) implements Comparable<Pancake> {
        @Override
        public int compareTo(Pancake o) {
            return this.flavor.compareTo(o.flavor);
        }
    }
}


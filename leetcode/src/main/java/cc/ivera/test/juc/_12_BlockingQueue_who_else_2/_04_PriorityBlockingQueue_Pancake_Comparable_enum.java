package cc.ivera.test.juc._12_BlockingQueue_who_else_2;


import cc.ivera.test.juc.SmallTool;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class _04_PriorityBlockingQueue_Pancake_Comparable_enum {

    public static void main(String[] args) {
//        BlockingQueue<Pancake> blockingQueue = new PriorityBlockingQueue<>(
//                3,
//                (o1, o2) -> o1.flavor.compareTo(o2.flavor)
//        );
        BlockingQueue<Pancake> blockingQueue = new PriorityBlockingQueue<>(
                3,
                Comparator.comparing(Pancake::flavor)
        );

        Thread xiaobai = new Thread(() -> {
            blockingQueue.offer(new Pancake(Flavor.UNPALATABLE));
            SmallTool.printTimeAndThread("做好第1个烧饼");

            blockingQueue.offer(new Pancake(Flavor.DELICIOUS));
            SmallTool.printTimeAndThread("做好第2个烧饼");

            blockingQueue.offer(new Pancake(Flavor.EDIBLE));
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

    private enum Flavor {
        DELICIOUS, // 好吃
        EDIBLE,     // 一般(能下口)
        UNPALATABLE // 难吃
    }

    private record Pancake(Flavor flavor) {
    }

}

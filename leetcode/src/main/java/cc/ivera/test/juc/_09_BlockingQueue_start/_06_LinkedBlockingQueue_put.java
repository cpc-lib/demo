package cc.ivera.test.juc._09_BlockingQueue_start;


import cc.ivera.test.juc.SmallTool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class _06_LinkedBlockingQueue_put {
    public static void main(String[] args) {
        BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>(1);

        try {
            blockingQueue.put("one");
            SmallTool.printTimeAndThread("one放进去了");

            blockingQueue.put("two");
            SmallTool.printTimeAndThread("two放进去了");

        } catch (InterruptedException e) {
            SmallTool.printTimeAndThread("取元素被中断");
        }
    }
}

package cc.ivera.test.juc._09_BlockingQueue_start;


import cc.ivera.test.juc.SmallTool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class _05_LinkedBlockingQueue_take {
    public static void main(String[] args) {
        BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>(3);

        try {
            blockingQueue.take();
        } catch (InterruptedException e) {
            SmallTool.printTimeAndThread("取元素被中断");
        }
    }
}

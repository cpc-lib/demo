package cc.ivera.test.juc._12_BlockingQueue_who_else_2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class _01_PriorityBlockingQueue_start {
    public static void main(String[] args) {
        BlockingQueue<Integer> blockingQueue = new PriorityBlockingQueue<>();

        blockingQueue.offer(3);
        blockingQueue.offer(1);
        blockingQueue.offer(2);

        while (!blockingQueue.isEmpty()) {
            System.out.println(blockingQueue.poll());
        }
    }
}


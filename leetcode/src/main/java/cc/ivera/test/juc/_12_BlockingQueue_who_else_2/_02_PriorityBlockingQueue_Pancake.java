package cc.ivera.test.juc._12_BlockingQueue_who_else_2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class _02_PriorityBlockingQueue_Pancake {

    public static void main(String[] args) {
        BlockingQueue<Pancake> blockingQueue = new PriorityBlockingQueue<>();

        blockingQueue.offer(new Pancake(0));
    }

    private record Pancake(
            /**
             * 美味程度
             *  0 --> 好吃
             *  1 --> 一般
             *  2 --> 难吃
             */
            Integer flavor) {
    }
}


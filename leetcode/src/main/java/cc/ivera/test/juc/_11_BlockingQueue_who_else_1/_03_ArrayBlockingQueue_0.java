package cc.ivera.test.juc._11_BlockingQueue_who_else_1;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class _03_ArrayBlockingQueue_0 {

    public static void main(String[] args) {
        BlockingQueue<String> shaobingQueue = new ArrayBlockingQueue<>(0, true);
    }
}

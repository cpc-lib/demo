package cc.ivera.test.juc._09_BlockingQueue_start;

import java.util.LinkedList;
import java.util.Queue;

public class _01_Queue_demo {
    public static void main(String[] args) {
        Queue<String> queue = new LinkedList<>();
        queue.offer("one");
        queue.offer("two");
        queue.offer("three");

        System.out.println("--------开始打印--------");
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println(queue.poll());
        System.out.println("--------结束打印--------");
    }
}

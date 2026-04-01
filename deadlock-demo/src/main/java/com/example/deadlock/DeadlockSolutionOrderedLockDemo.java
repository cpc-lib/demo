package com.example.deadlock;

/**
 * 解决方案 1：
 * 所有线程按固定顺序获取锁，避免循环等待
 * 无论哪个线程执行，都先获取 LOCK_A，再获取 LOCK_B
 */
public class DeadlockSolutionOrderedLockDemo {

    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> doWork("t1"), "t1");
        Thread t2 = new Thread(() -> doWork("t2"), "t2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("程序执行结束，没有发生死锁");
    }

    private static void doWork(String threadName) {
        synchronized (LOCK_A) {
            System.out.println(threadName + " 获取到 LOCK_A");
            sleep(300);
            synchronized (LOCK_B) {
                System.out.println(threadName + " 获取到 LOCK_B");
                System.out.println(threadName + " 执行业务逻辑");
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
